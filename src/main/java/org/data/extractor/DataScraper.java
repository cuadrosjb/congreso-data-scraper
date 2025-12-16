package org.data.extractor;


import org.data.extractor.service.PageScraper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DataScraper {


    @Autowired
    PageScraper pageScraper;

    public static void main(String[] args) {
        SpringApplication.run(DataScraper.class, args);
    }

    @Bean
    public CommandLineRunner runMyStartupLogic() {
        return args -> {
            pageScraper.loadData();
        };
    }
}
