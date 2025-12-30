package org.data.extractor.configuration;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Configuration
public class SeleniumConfiguration {

    @Bean
    @Scope("prototype")
    public WebDriver webDriver() {
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
        WebDriver driver = new ChromeDriver(options);
        driver.manage().window().maximize(); // Optional: Maximize window
        return driver;
    }


    @Bean
    @Scope("prototype")
    public WebDriverWait wait(WebDriver driver) {
        return new WebDriverWait(driver, Duration.ofSeconds(15));
    }


}
