package org.data.extractor.service;

import org.data.extractor.pojo.CongressMember;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SeleniumCongressFetcher {

    private static final String BASE_URL = "https://www.congreso.gob.pe/pleno/congresistas/";
    private WebDriver driver;
    private WebDriverWait wait;

    public void initializeDriver() {
        System.out.println("Initializing Chrome WebDriver...");

        ChromeOptions options = new ChromeOptions();
        // Comment out headless to see what's happening
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
//        options.addArguments("--headless=new");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        System.out.println("✓ WebDriver initialized\n");
    }

    public void loadInitialPage() {
        System.out.println("Loading initial page...");
        driver.get(BASE_URL);

        // Wait for page to fully load
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("table")));

        System.out.println("✓ Page loaded\n");
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
        return parseMembers(pageSource, periodName);
    }


    public List<CongressMember> fetchMembersForPeriod(String periodName) throws InterruptedException {
        System.out.println("  → Looking for period dropdown...");

        try {


//            WebElement periodDropdown = driver.findElement(By.name("idRegistroPadre"));
//            System.out.println("  ✓ Found period dropdown");
//
//            Select dropdown = new Select(periodDropdown);
//
//            // Check if the period is already selected
//            WebElement currentlySelected = dropdown.getFirstSelectedOption();
//            String currentPeriod = currentlySelected.getText().trim();
//
//            if (currentPeriod.equals(periodName)) {
//                System.out.println("  → Period already selected, skipping reload");
//
//                // Parse the current page directly
//                String pageSource = driver.getPageSource();
//                return parseMembers(pageSource, periodName);
//            }


            WebElement element = driver.findElement(By.name("idRegistroPadre"));

            Select dropdown = new Select(element);
            List<WebElement> options = dropdown.getOptions();

            for (WebElement option : options) {
                // Item already selected

//                String selected = option.getAttribute("selected");
                if (Boolean.parseBoolean(option.getAttribute("selected")) && option.getText().trim().equals(periodName)) {
                    System.out.printf("  ✓ Period already selected -- Skipping %s reload.", periodName);
                    String pageSource = driver.getPageSource();
                    return parseMembers(pageSource, periodName);
                }
                if (option.getText().trim().equals(periodName)) {
                    System.out.println("  ✓ Found period dropdown");
                    return reloadPageContent(dropdown, periodName);
                }
            }
            // throw new Exception("Period not found in dropdown");
            System.out.println("  ✗ Period not found in dropdown: " + periodName);
            return Collections.emptyList();

        } catch (Exception e) {
            System.out.println("  ✗ Error selecting period: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    private List<CongressMember> parseMembers(String html, String periodName) {
        System.out.println("  → Parsing HTML...");

        Document doc = Jsoup.parse(html);
        Element table = doc.selectFirst("table");

        if (table == null) {
            System.out.println("  ✗ No table found on page");
            return List.of();
        }

        List<CongressMember> members = new ArrayList<>();
        Elements rows = table.select("tr");

        System.out.println("  → Processing " + (rows.size() - 1) + " rows...");

        // Skip header row
        for (int i = 1; i < rows.size(); i++) {
            Element row = rows.get(i);
            Elements cols = row.select("td");

            if (cols.size() >= 3) {
                try {
                    Element nameLink = cols.get(1).selectFirst("a");
                    if (nameLink == null) continue;

                    String fullName = nameLink.text().trim();
                    String profileUrl = BASE_URL + nameLink.attr("href");
                    String group = cols.get(2).text().trim();

                    Element emailLink = cols.get(3).selectFirst("a");
                    String email = emailLink != null ? emailLink.text().trim() : "";

                    // Determine active status based on period
                    boolean isActive = periodName.contains("2021") && periodName.contains("2026");
                    String status = isActive ? "active" : "retired";

                    members.add(new CongressMember(
                            fullName,
                            group,
                            email,
                            profileUrl
//                            status,
//                            isActive
                    ));

                } catch (Exception e) {
                    System.err.println("  ⚠ Error parsing row " + i + ": " + e.getMessage());
                }
            }
        }

        System.out.println("  ✓ Parsed " + members.size() + " members");
        return members;
    }

    public void cleanup() {
        if (driver != null) {
            System.out.println("\nCleaning up WebDriver...");
            driver.quit();
            System.out.println("✓ WebDriver closed");
        }
    }


    public static void main(String[] args) throws Exception {
        var fetcher = new SeleniumCongressFetcher();

        // List of periods to fetch
        var periods = List.of(
                "Parlamentario 2021 - 2026",
                "Parlamentario 2016 - 2021",
                "Parlamentario 2011 - 2016",
                "Parlamentario 2006 - 2011",
                "Parlamentario 2001 - 2006",
                "Parlamentario 2000 - 2001",
                "Parlamentario 1995 - 2000",
                "CCD 1992 -1995"
        );

        System.out.println("Starting Selenium-based congress member fetcher");
        System.out.println("=".repeat(70) + "\n");

        fetcher.initializeDriver();

        try {
            // First, get the actual dropdown to see available options
            fetcher.loadInitialPage();

            for (String period : periods) {
                System.out.println("\n" + "=".repeat(70));
                System.out.println("Fetching period: " + period);
                System.out.println("=".repeat(70));

                var members = fetcher.fetchMembersForPeriod(period);

                System.out.println("\nFound " + members.size() + " members");

                if (!members.isEmpty()) {
                    // Display sample
                    System.out.println("\nSample members:");
                    members.stream()
                            .limit(1)
                            .forEach(m -> System.out.printf("  %-40s  %s%n", m.name(), m.parliamentaryGroup()));

                }

                // Wait between requests to be polite
                Thread.sleep(2000);
            }

            System.out.println("\n" + "=".repeat(70));
            System.out.println("✓ All periods fetched successfully!");
            System.out.println("=".repeat(70));

        } finally {
            fetcher.cleanup();
        }
    }


}
