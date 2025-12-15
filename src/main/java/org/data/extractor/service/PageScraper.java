package org.data.extractor.service;

import lombok.RequiredArgsConstructor;
import org.data.extractor.entity.Webpage;
import org.data.extractor.pojo.ParliamentaryPeriod;
import org.data.extractor.repository.WebpageRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;

@Service
@RequiredArgsConstructor
public class PageScraper {

    private static final String BASE_URL = "https://www.congreso.gob.pe/pleno/congresistas/";

    HttpClient client;

    WebpageRepository repository;


    /**
     * Extract all parliamentary periods from the dropdown on the webpage
     */
    public String extractPeriodsFromPage() throws IOException, InterruptedException {

        // Fetch the page
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

        // Parse HTML
        return response.body();
    }

    private Optional<String> getTheLatestFetchedPage() {
        return repository.findAll().stream()
                .filter(webpage -> webpage.getInsertedTime().isAfter(Instant.now().minus(1, ChronoUnit.DAYS)))
                .map(Webpage::getPageBlob)
                .findFirst();
    }

    public void loadData() throws IOException, InterruptedException {
        // Implementation for loading data

        String body = getTheLatestFetchedPage().orElseGet(() -> {
            try {
                return extractPeriodsFromPage();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        Jsoup.parse(body);


    }

    private boolean isDuplicate(List<ParliamentaryPeriod> periods, ParliamentaryPeriod newPeriod) {
        return periods.stream()
                .anyMatch(p -> p.name().equals(newPeriod.name()));
    }

    /**
     * Parse a period string like "Parlamentario 2021 - 2026" into a ParliamentaryPeriod
     */
//    private ParliamentaryPeriod parsePeriod(String periodText) {
//        Matcher matcher = YEAR_PATTERN.matcher(periodText);
//
//        if (matcher.find()) {
//            int startYear = Integer.parseInt(matcher.group(1));
//            int endYear = Integer.parseInt(matcher.group(2));
//
//            // The current period is 2021-2026
//            boolean isActive = periodText.contains("2021") && periodText.contains("2026");
//
//            return new ParliamentaryPeriod(periodText, startYear, endYear, isActive);
//        }
//
//        return null;
//    }

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
    public static void main(String[] args) throws IOException, InterruptedException {
        var scraper = new PageScraper();

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

}
