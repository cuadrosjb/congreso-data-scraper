-- ============================================================
-- PostgreSQL Schema for Peru Congress Data
-- ============================================================

-- Drop tables if they exist (for clean setup)
DROP TABLE IF EXISTS congress_members CASCADE;
DROP TABLE IF EXISTS parliamentary_periods CASCADE;
DROP TABLE IF EXISTS html_documents CASCADE;
DROP TABLE IF EXISTS parliamentary_groups CASCADE;

-- ============================================================
-- Table: parliamentary_periods
-- Stores information about different congressional periods
-- ============================================================
CREATE TABLE parliamentary_periods (
                                       period_id SERIAL PRIMARY KEY,
                                       period_name VARCHAR(100) NOT NULL UNIQUE,
                                       start_year INTEGER NOT NULL,
                                       end_year INTEGER NOT NULL,
                                       description TEXT,
                                       is_active BOOLEAN DEFAULT FALSE,
                                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index for quick lookups
CREATE INDEX idx_periods_active ON parliamentary_periods(is_active);
CREATE INDEX idx_periods_years ON parliamentary_periods(start_year, end_year);

-- ============================================================
-- Table: parliamentary_groups
-- Stores political parties/groups
-- ============================================================
CREATE TABLE parliamentary_groups (
                                      group_id SERIAL PRIMARY KEY,
                                      group_name VARCHAR(200) NOT NULL UNIQUE,
                                      abbreviation VARCHAR(50),
                                      description TEXT,
                                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- Table: congress_members
-- Stores individual congress members
-- ============================================================
CREATE TABLE congress_members (
                                  member_id SERIAL PRIMARY KEY,
                                  period_id INTEGER NOT NULL REFERENCES parliamentary_periods(period_id) ON DELETE CASCADE,
                                  group_id INTEGER REFERENCES parliamentary_groups(group_id) ON DELETE SET NULL,

    -- Personal information
                                  full_name VARCHAR(200) NOT NULL,
                                  first_name VARCHAR(100),
                                  last_name VARCHAR(100),

    -- Contact information
                                  email VARCHAR(200),
                                  profile_url TEXT,

    -- Electoral information
                                  electoral_district VARCHAR(100),
                                  gender VARCHAR(20),

    -- Status information
                                  status VARCHAR(50) DEFAULT 'active', -- active, retired, suspended, deceased, etc.
                                  is_active BOOLEAN DEFAULT TRUE,
                                  condition_notes TEXT, -- For details about status

    -- Dates
                                  term_start_date DATE,
                                  term_end_date DATE,
                                  last_active_date DATE,

    -- Metadata
                                  scraped_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Ensure unique member per period
                                  CONSTRAINT unique_member_per_period UNIQUE (period_id, full_name)
);

-- Indexes for common queries
CREATE INDEX idx_members_period ON congress_members(period_id);
CREATE INDEX idx_members_group ON congress_members(group_id);
CREATE INDEX idx_members_status ON congress_members(status);
CREATE INDEX idx_members_active ON congress_members(is_active);
CREATE INDEX idx_members_name ON congress_members(full_name);
CREATE INDEX idx_members_district ON congress_members(electoral_district);

-- Full-text search index for names
CREATE INDEX idx_members_name_fulltext ON congress_members
    USING gin(to_tsvector('spanish', full_name));

-- ============================================================
-- Table: html_documents
-- Stores raw HTML documents fetched from the website
-- ============================================================
CREATE TABLE html_documents (
                                document_id SERIAL PRIMARY KEY,
                                period_id INTEGER REFERENCES parliamentary_periods(period_id) ON DELETE CASCADE,

    -- Document metadata
                                url TEXT NOT NULL,
                                document_type VARCHAR(50) NOT NULL, -- 'list_page', 'member_profile', 'period_page'

    -- Content
                                html_content TEXT NOT NULL,
                                content_hash VARCHAR(64), -- SHA-256 hash to detect changes

    -- HTTP metadata
                                http_status_code INTEGER,
                                content_length INTEGER,

    -- Timestamps
                                fetched_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Ensure we don't duplicate documents
                                CONSTRAINT unique_document_per_fetch UNIQUE (url, fetched_at)
);

-- Indexes
CREATE INDEX idx_documents_period ON html_documents(period_id);
CREATE INDEX idx_documents_type ON html_documents(document_type);
CREATE INDEX idx_documents_url ON html_documents(url);
CREATE INDEX idx_documents_hash ON html_documents(content_hash);
CREATE INDEX idx_documents_fetched ON html_documents(fetched_at DESC);

-- ============================================================
-- Trigger: Update updated_at timestamp automatically
-- ============================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_parliamentary_periods_updated_at
    BEFORE UPDATE ON parliamentary_periods
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_congress_members_updated_at
    BEFORE UPDATE ON congress_members
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- Views: Useful queries
-- ============================================================

-- View: Active members with all details
CREATE VIEW active_congress_members AS
SELECT
    cm.member_id,
    cm.full_name,
    cm.email,
    pp.period_name,
    pg.group_name as parliamentary_group,
    cm.electoral_district,
    cm.gender,
    cm.status,
    cm.term_start_date,
    cm.term_end_date
FROM congress_members cm
         JOIN parliamentary_periods pp ON cm.period_id = pp.period_id
         LEFT JOIN parliamentary_groups pg ON cm.group_id = pg.group_id
WHERE cm.is_active = TRUE
ORDER BY cm.full_name;

-- View: Member counts by period and group
CREATE VIEW member_statistics AS
SELECT
    pp.period_name,
    pg.group_name,
    COUNT(cm.member_id) as member_count,
    COUNT(CASE WHEN cm.is_active THEN 1 END) as active_count,
    COUNT(CASE WHEN cm.status = 'retired' THEN 1 END) as retired_count
FROM parliamentary_periods pp
         LEFT JOIN congress_members cm ON pp.period_id = cm.period_id
         LEFT JOIN parliamentary_groups pg ON cm.group_id = pg.group_id
GROUP BY pp.period_name, pg.group_name
ORDER BY pp.period_name DESC, member_count DESC;

-- View: Latest HTML documents by type
CREATE VIEW latest_documents AS
SELECT DISTINCT ON (document_type, period_id)
        document_id,
        period_id,
        url,
        document_type,
        fetched_at,
        content_length
        FROM html_documents
        ORDER BY document_type, period_id, fetched_at DESC;

-- ============================================================
-- Sample data insertion
-- ============================================================

-- Insert parliamentary periods
INSERT INTO parliamentary_periods (period_name, start_year, end_year, is_active) VALUES
                                                                                     ('Parlamentario 2021 - 2026', 2021, 2026, TRUE),
                                                                                     ('Parlamentario 2016 - 2021', 2016, 2021, FALSE),
                                                                                     ('Parlamentario 2011 - 2016', 2011, 2016, FALSE),
                                                                                     ('Parlamentario 2006 - 2011', 2006, 2011, FALSE),
                                                                                     ('Parlamentario 2001 - 2006', 2001, 2006, FALSE),
                                                                                     ('Parlamentario 2000 - 2001', 2000, 2001, FALSE),
                                                                                     ('Parlamentario 1995 - 2000', 1995, 2000, FALSE),
                                                                                     ('CCD 1992 - 1995', 1992, 1995, FALSE);

-- Insert some common parliamentary groups
INSERT INTO parliamentary_groups (group_name, abbreviation) VALUES
                                                                ('FUERZA POPULAR', 'FP'),
                                                                ('ALIANZA PARA EL PROGRESO', 'APP'),
                                                                ('ACCIÓN POPULAR', 'AP'),
                                                                ('PERÚ LIBRE', 'PL'),
                                                                ('RENOVACIÓN POPULAR', 'RP'),
                                                                ('AVANZA PAÍS - PARTIDO DE INTEGRACIÓN SOCIAL', 'AP-PIS'),
                                                                ('PODEMOS PERÚ', 'PP'),
                                                                ('SOMOS PERÚ', 'SP'),
                                                                ('NO AGRUPADO', 'NA');

-- ============================================================
-- Useful queries (examples)
-- ============================================================

-- Query 1: Count active vs retired members per period
-- SELECT
--     pp.period_name,
--     COUNT(*) as total_members,
--     COUNT(CASE WHEN cm.is_active THEN 1 END) as active,
--     COUNT(CASE WHEN NOT cm.is_active THEN 1 END) as retired
-- FROM parliamentary_periods pp
-- LEFT JOIN congress_members cm ON pp.period_id = cm.period_id
-- GROUP BY pp.period_name
-- ORDER BY pp.start_year DESC;

-- Query 2: Find all members who served in multiple periods
-- SELECT
--     full_name,
--     COUNT(DISTINCT period_id) as periods_served,
--     STRING_AGG(DISTINCT pp.period_name, ', ' ORDER BY pp.period_name) as periods
-- FROM congress_members cm
-- JOIN parliamentary_periods pp ON cm.period_id = pp.period_id
-- GROUP BY full_name
-- HAVING COUNT(DISTINCT period_id) > 1
-- ORDER BY periods_served DESC;

-- Query 3: Get latest HTML document for current period
-- SELECT * FROM latest_documents
-- WHERE document_type = 'list_page'
-- ORDER BY fetched_at DESC
-- LIMIT 1;

COMMENT ON TABLE parliamentary_periods IS 'Stores congressional periods/terms';
COMMENT ON TABLE congress_members IS 'Individual congress members with status tracking';
COMMENT ON TABLE html_documents IS 'Raw HTML documents for audit and re-parsing';
COMMENT ON TABLE parliamentary_groups IS 'Political parties and parliamentary groups';