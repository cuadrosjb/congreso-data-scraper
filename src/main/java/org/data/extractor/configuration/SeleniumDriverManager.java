package org.data.extractor.configuration;

import jakarta.annotation.PreDestroy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SeleniumDriverManager {
    private final ApplicationContext context;
    private final List<WebDriver> activeDrivers = new ArrayList<>();

    public SeleniumDriverManager(ApplicationContext context) {
        this.context = context;
    }

    /**
     * Get a new WebDriver instance
     */
    public WebDriver getDriver() {
        WebDriver driver = context.getBean(WebDriver.class);
        activeDrivers.add(driver);
        return driver;
    }

    /**
     * Get WebDriverWait for a specific driver
     */
    public WebDriverWait getWait(WebDriver driver) {
        return context.getBean(WebDriverWait.class, driver);
    }

    /**
     * Close a specific driver
     */
    public void closeDriver(WebDriver driver) {
        if (driver != null) {
            driver.quit();
            activeDrivers.remove(driver);
        }
    }

    /**
     * Cleanup all drivers on shutdown
     */
    @PreDestroy
    public void cleanup() {
        for (WebDriver driver : activeDrivers) {
            try {
                driver.quit();
            } catch (Exception e) {
                // Log error
            }
        }
        activeDrivers.clear();
    }



}
