package org.data.extractor;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

    void main() throws IOException, InterruptedException {
        var scraper = new ScraperCongresoClaude();
        var members = scraper.scrapeCongressMembers();

        System.out.println("Found " + members.size() + " congress members\n");

        // Display first 5 members
        System.out.println("First 5 members:");
        members
                .forEach(m -> System.out.println("- %s (%s)".formatted(
                        m.name(), m.parliamentaryGroup())));

        // Save to JSON
        scraper.saveToJson(members, "congress_members.json");

        // Save to CSV
        scraper.saveToCsv(members, "congress_members.csv");

        // Summary by parliamentary group
        System.out.println("\nMembers by Parliamentary Group:");
        members.stream()
                .collect(Collectors.groupingBy(CongressMember::parliamentaryGroup, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(e -> System.out.println("%s: %d".formatted(e.getKey(), e.getValue())));
    }

    record CongressMember(
            String name,
            String parliamentaryGroup,
            String email,
            String profileUrl
    ) {}
}
