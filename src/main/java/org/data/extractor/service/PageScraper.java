package org.data.extractor.service;

import lombok.RequiredArgsConstructor;
import org.data.extractor.entity.Webpage;
import org.data.extractor.repository.WebpageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PageScraper {

    @Value("${congreso.base-url}")
    private String BASE_URL;

    private final HttpClient client;

    private final WebpageRepository repository;


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

        String body = getTheLatestFetchedPage().orElseGet(() -> {
            try {
                return extractPeriodsFromPage();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        System.out.println(body);

//        Jsoup.parse(body);


    }
//            }
//        }
//    }
//    public static void main(String[] args) throws IOException, InterruptedException {
//        var scraper = new PageScraper();
//
//        // STEP 1: Extract periods from dropdown
//        System.out.println("STEP 1: Extracting parliamentary periods from dropdown");
//        System.out.println("=".repeat(70));
//
//        var periods = scraper.extractPeriodsFromPage();
//
//        System.out.println("\nFound " + periods.size() + " parliamentary periods:\n");
//
//        for (var period : periods) {
//            System.out.printf("  %-35s  Start: %4d  End: %4d  Active: %s%n",
//                    period.name(),
//                    period.startYear(),
//                    period.endYear(),
//                    period.isActive() ? "✓" : " "
//            );
//        }
//
//        // STEP 2: Save to PostgreSQL
//        System.out.println("\n" + "=".repeat(70));
//        System.out.println("STEP 2: Saving to PostgreSQL database");
//        System.out.println("=".repeat(70) + "\n");
//
////    scraper.savePeriodsToDB(periods);
//
//        System.out.println("\n✓ All periods saved successfully!");
//    }

}
