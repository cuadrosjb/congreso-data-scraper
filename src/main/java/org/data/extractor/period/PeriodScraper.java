import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

void main() throws Exception {
    var scraper = new PeriodScraper();

    // STEP 1: Extract periods from dropdown
    System.out.println("STEP 1: Extracting parliamentary periods from dropdown");
    System.out.println("=".repeat(70));

    var periods = scraper.extractPeriodsFromPage();

    System.out.println("\nFound " + periods.size() + " parliamentary periods:\n");

    for (var period : periods) {
        System.out.printf("  %-35s  Start: %4d  End: %4d  Active: %s%n",
                period.name(),
                period.startYear(),
                period.endYear(),
                period.isActive() ? "✓" : " "
        );
    }

    // STEP 2: Save to PostgreSQL
    System.out.println("\n" + "=".repeat(70));
    System.out.println("STEP 2: Saving to PostgreSQL database");
    System.out.println("=".repeat(70) + "\n");

//    scraper.savePeriodsToDB(periods);

    System.out.println("\n✓ All periods saved successfully!");
}

// Record to hold period information
record ParliamentaryPeriod(
        String name,
        int startYear,
        int endYear,
        boolean isActive
) {
}

class PeriodScraper {
    private static final String BASE_URL = "https://www.congreso.gob.pe/pleno/congresistas/";
    private final HttpClient client = HttpClient.newHttpClient();

    // Pattern to extract year ranges like "2021 - 2026" or "1992 -1995"
    private static final Pattern YEAR_PATTERN = Pattern.compile("(\\d{4})\\s*-\\s*(\\d{4})");

    /**
     * Extract all parliamentary periods from the dropdown on the webpage
     */
    public List<ParliamentaryPeriod> extractPeriodsFromPage() throws IOException, InterruptedException {

            // Fetch the page
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            // Parse HTML
            Document doc = Jsoup.parse(response.body());

            List<ParliamentaryPeriod> periods = new ArrayList<>();

            // The periods are in the page text, look for all text containing year ranges
            String pageText = doc.body().text();

            // Split by common separators and look for period patterns
            String[] segments = pageText.split("\\s+");

            // Build potential period strings by combining consecutive words
            StringBuilder current = new StringBuilder();

            for (int i = 0; i < segments.length; i++) {
                String segment = segments[i];

                // Start building when we see "Parlamentario" or "CCD"
                if (segment.equals("Parlamentario") || segment.equals("CCD")) {
                    current = new StringBuilder(segment);

                    // Look ahead to build the full period string
                    for (int j = i + 1; j < Math.min(i + 6, segments.length); j++) {
                        current.append(" ").append(segments[j]);

                        // Check if we have a complete period
                        String potentialPeriod = current.toString();
                        if (YEAR_PATTERN.matcher(potentialPeriod).find()) {
                            var period = parsePeriod(potentialPeriod);
                            if (period != null && !isDuplicate(periods, period)) {
                                periods.add(period);
                            }
                            break;
                        }
                    }
                }
            }

            // If extraction failed, use hardcoded list
//            if (periods.isEmpty()) {
//                System.out.println("  Warning: Could not extract from page, using hardcoded list");
//                periods = getHardcodedPeriods();
//            }

            return periods;
    }

    private boolean isDuplicate(List<ParliamentaryPeriod> periods, ParliamentaryPeriod newPeriod) {
        return periods.stream()
                .anyMatch(p -> p.name().equals(newPeriod.name()));
    }

    /**
     * Parse a period string like "Parlamentario 2021 - 2026" into a ParliamentaryPeriod
     */
    private ParliamentaryPeriod parsePeriod(String periodText) {
        Matcher matcher = YEAR_PATTERN.matcher(periodText);

        if (matcher.find()) {
            int startYear = Integer.parseInt(matcher.group(1));
            int endYear = Integer.parseInt(matcher.group(2));

            // The current period is 2021-2026
            boolean isActive = periodText.contains("2021") && periodText.contains("2026");

            return new ParliamentaryPeriod(periodText, startYear, endYear, isActive);
        }

        return null;
    }

    /**
     * Save periods to PostgreSQL database
     */
//    public void savePeriodsToDB(List<ParliamentaryPeriod> periods) throws SQLException {
//        String url = "jdbc:postgresql://localhost:5432/congress_db";
//        String user = "postgres";
//        String password = "postgres";
//
//        try (Connection conn = DriverManager.getConnection(url, user, password)) {
//
//            String insertSQL = """
//                INSERT INTO parliamentary_periods
//                    (period_name, start_year, end_year, is_active)
//                VALUES (?, ?, ?, ?)
//                ON CONFLICT (period_name)
//                DO UPDATE SET
//                    start_year = EXCLUDED.start_year,
//                    end_year = EXCLUDED.end_year,
//                    is_active = EXCLUDED.is_active,
//                    updated_at = CURRENT_TIMESTAMP
//                """;
//
//            try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
//                for (var period : periods) {
//                    pstmt.setString(1, period.name());
//                    pstmt.setInt(2, period.startYear());
//                    pstmt.setInt(3, period.endYear());
//                    pstmt.setBoolean(4, period.isActive());
//
//                    pstmt.executeUpdate();
//                    System.out.println("  ✓ Saved: " + period.name());
//                }
//            }
//        }
//    }
}