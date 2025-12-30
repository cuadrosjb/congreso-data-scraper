package org.data.extractor.service;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.data.extractor.configuration.SeleniumDriverManager;
import org.data.extractor.entity.Webpage;
import org.data.extractor.pojo.CongressMember;
import org.data.extractor.repository.WebpageRepository;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SeleniumCongressFetcher {

    @Value("${congreso.base-url}")
    private String BASE_URL;

    private final SeleniumDriverManager driverManager;

    private final WebpageRepository webpageRepository;

    private WebDriverWait wait;
    private WebDriver driver;

    public void loadInitialPage() {
        driver = driverManager.getDriver();
        driver.get(BASE_URL);
        wait = driverManager.getWait(driver);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("table")));
    }

    private List<CongressMember> reloadPageContent(Select option, String periodName) throws InterruptedException {
        System.out.println("  → Selecting period: " + periodName);
        option.selectByVisibleText(periodName);

        System.out.println("  → Waiting for page to reload...");

        Thread.sleep(3000);

        // Wait for table to be present again (it should reload)
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("table")));
        Thread.sleep(1000); // Extra buffer

        System.out.println("  ✓ Page reloaded");

        // Parse the updated page
        String pageSource = driver.getPageSource();

        //check if already saved
        if (!isPeriodAlreadyLoaded(periodName)) {
            System.out.println("  → Saving: Webpage Repository period " + periodName + "...");
            webpageRepository.save(Webpage.builder()
                    .pageBlob(pageSource)
                    .parliamentaryPeriod(periodName)
                    .build());
        }

        return parseMembers(pageSource, periodName);
    }


    private Boolean isPeriodAlreadyLoaded(String periodName) {
        return webpageRepository.findAllFormatted()
                .stream()
                .anyMatch(periodName::equalsIgnoreCase);
    }


    public List<CongressMember> saveWebpageForParliamentaryPeriod(String periodName) {


        if (isPeriodAlreadyLoaded(periodName)) {
            Optional<String> document = webpageRepository.findAll()
                    .stream()
                    .filter(webpage -> periodName.equalsIgnoreCase(webpage.getParliamentaryPeriod()))
                    .map(Webpage::getPageBlob)
                    .findFirst();

            return document.map(s -> parseMembers(s, periodName)).orElse(Collections.emptyList());

        }

        System.out.println("Processing parliamentary period: " + periodName);



        loadInitialPage();

        try {
            WebElement element = driver.findElement(By.name("idRegistroPadre"));

            Select dropdown = new Select(element);
            List<WebElement> options = dropdown.getOptions();

            for (WebElement option : options) {
                if (Boolean.parseBoolean(option.getAttribute("selected")) && option.getText().trim().equals(periodName)) {
                    String pageSource = driver.getPageSource();

                    //check if already saved
                    if (!isPeriodAlreadyLoaded(periodName)) {
                        System.out.println("  → Saving: Webpage Repository period " + periodName + "...");
                        webpageRepository.save(Webpage.builder()
                                .pageBlob(pageSource)
                                .parliamentaryPeriod(periodName)
                                .build());
                    }

                    return parseMembers(pageSource, periodName);
                }
                if (option.getText().trim().equals(periodName)) {
                    return reloadPageContent(dropdown, periodName);
                }
            }
            // throw new Exception("Period not found in dropdown");
            return Collections.emptyList();

        } catch (Exception e) {
            System.out.println("  ✗ Error selecting period: " + e.getMessage());
            return List.of();
        }

    }

    private List<CongressMember> parseMembers(String html, String periodName) {

        return Collections.emptyList();
    }

    @PreDestroy
    public void cleanup() {
        if (driver != null) {
            System.out.println("\nCleaning up WebDriver...");
            driver.quit();
            System.out.println("✓ WebDriver closed");
        }
    }


}
