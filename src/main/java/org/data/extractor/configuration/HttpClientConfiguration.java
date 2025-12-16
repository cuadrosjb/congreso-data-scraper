package org.data.extractor.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;


@Configuration
public class HttpClientConfiguration {


    @Bean
    public HttpClient httpClient() {
        // Configure and return a single, reusable instance
        return HttpClient.newHttpClient();
    }
}
