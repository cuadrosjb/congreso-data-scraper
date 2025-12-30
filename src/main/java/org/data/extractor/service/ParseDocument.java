package org.data.extractor.service;

import lombok.RequiredArgsConstructor;
import org.data.extractor.pojo.CongressMember;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ParseDocument {

    @Value("${congreso.base-url}")
    private String BASE_URL;

    private final PageScraper pageScraper;

    public List<CongressMember> getAllCongressMembers() {

        Document doc = pageScraper.loadDocument();

        Element table = doc.selectFirst("table");

        if (table == null) {
            throw new IllegalStateException("Table not found on page");
        }

        List<CongressMember> members = new ArrayList<>();
        Elements rows = table.select("tr");

        // Skip header row
        for (int i = 1; i < rows.size(); i++) {
            Element row = rows.get(i);
            Elements cols = row.select("td");

            if (cols.size() >= 3) {
                // Extract name and profile URL
                Element nameLink = cols.get(1).selectFirst("a");
                if (nameLink == null) continue;

                String name = nameLink.text().trim();
                String profileUrl = BASE_URL + nameLink.attr("href");

                // Extract parliamentary group
                String group = cols.get(2).text().trim();

                // Extract email
                Element emailLink = cols.get(3).selectFirst("a");
                String email = emailLink != null ? emailLink.text().trim() : "";

                members.add(new CongressMember(name, group, email, profileUrl));
            }
        }

        return members;
    }


    public List<CongressMember> getAllCongressMembersForParlamentaryPeriod(String parlamentaryPeriod) throws IOException, InterruptedException {

        Document doc = pageScraper.loadDocument();
        Element table = doc.selectFirst("table");

        if (table == null) {
            throw new IllegalStateException("Table not found on page");
        }

        List<CongressMember> members = new ArrayList<>();
        Elements rows = table.select("tr");

        // Skip header row
        for (int i = 1; i < rows.size(); i++) {
            Element row = rows.get(i);
            Elements cols = row.select("td");

            if (cols.size() >= 3) {
                // Extract name and profile URL
                Element nameLink = cols.get(1).selectFirst("a");
                if (nameLink == null) continue;

                String name = nameLink.text().trim();
                String profileUrl = BASE_URL + nameLink.attr("href");

                // Extract parliamentary group
                String group = cols.get(2).text().trim();

                // Extract email
                Element emailLink = cols.get(3).selectFirst("a");
                String email = emailLink != null ? emailLink.text().trim() : "";

                members.add(new CongressMember(name, group, email, profileUrl));
            }
        }

        return members;
    }


    public List<String> getAllParliamentaryPeriods()  {
        Document doc = pageScraper.loadDocument();
        Element periodSelect = doc.selectFirst("select");
        if (periodSelect == null) {
            System.out.println("Warning: Period dropdown not found, using hardcoded list");
            return Collections.emptyList();
        }
        Elements options = periodSelect.select("option");
        return options.eachText();
    }

}
