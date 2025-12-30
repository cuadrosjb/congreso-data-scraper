package org.data.extractor.service;


import lombok.RequiredArgsConstructor;
import org.data.extractor.entity.Webpage;
import org.data.extractor.repository.WebpageRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;


@Service
@RequiredArgsConstructor
public class WebpageRepositoryService {

    private final WebpageRepository repository;

    public Boolean checkIfPageExists(String content) {

        return repository.findAll()
                .stream()
                .filter(webpage -> webpage.getInsertedTime().isAfter(Instant.now().minus(1, ChronoUnit.DAYS)))
                .filter(webpage -> content.equalsIgnoreCase(webpage.getParliamentaryPeriod()))
                .map(Webpage::getPageBlob)
                .findFirst()
                .isPresent();
    }


}