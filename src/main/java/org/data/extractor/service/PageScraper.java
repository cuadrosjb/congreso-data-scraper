package org.data.extractor.service;

import lombok.RequiredArgsConstructor;
import org.data.extractor.entity.Webpage;
import org.data.extractor.repository.WebpageRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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

        System.out.println("Fetching page from " + BASE_URL);
        // Fetch the page
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

        repository.save(Webpage.builder().pageBlob(response.body()).build());

        return response.body();
    }

    private Optional<String> getTheLatestFetchedPage() {
        return repository.findAll().stream()
                .filter(webpage -> webpage.getInsertedTime().isAfter(Instant.now().minus(1, ChronoUnit.DAYS)))
                .map(Webpage::getPageBlob)
                .peek(s -> System.out.println("Found in DB"))
                .findFirst();
    }

    public Document loadDocument() {

        String body = getTheLatestFetchedPage().orElseGet(() -> {
            try {
                return extractPeriodsFromPage();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        Jsoup.parse(body);

        return Jsoup.parse(body);
    }

}
