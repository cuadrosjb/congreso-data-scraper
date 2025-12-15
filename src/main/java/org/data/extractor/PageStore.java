package org.data.extractor;

import lombok.RequiredArgsConstructor;
import org.data.extractor.repository.WebpageRepository;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class PageStore {

    private static final String BASE_URL = "https://www.congreso.gob.pe/pleno/congresistas/";
    private final HttpClient client = HttpClient.newHttpClient();


    private final WebpageRepository webpageRepository;



    private void getCongressPage() throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(BASE_URL)).GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    }


    private boolean checkIfPageLoadedToday() {
        return webpageRepository.findAll().stream()
                .map(webpage -> LocalDate.ofInstant(webpage.getInsertedTime(), ZoneId.systemDefault()))
                .anyMatch(insertedDate -> insertedDate.equals(LocalDate.now(ZoneId.systemDefault())));
    }






}
