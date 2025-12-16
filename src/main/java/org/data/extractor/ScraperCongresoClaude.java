package org.data.extractor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


public class ScraperCongresoClaude {


    private static final String BASE_URL = "https://www.congreso.gob.pe/pleno/congresistas/";

    public List<Main.CongressMember> scrapeCongressMembers() throws IOException, InterruptedException {

        // Fetch the page using Java 25 HttpClient
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Parse HTML with Jsoup
        String body = response.body();
        Document doc = Jsoup.parse(body);
        Element table = doc.selectFirst("table");

        if (table == null) {
            throw new IllegalStateException("Table not found on page");
        }

        List<Main.CongressMember> members = new ArrayList<>();
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

                members.add(new Main.CongressMember(name, group, email, profileUrl));
            }
        }

        return members;
    }

    public void saveToJson(List<Main.CongressMember> members, String filename) throws IOException, IOException {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        String json = gson.toJson(members);
        Files.writeString(Path.of(filename), json);
        System.out.println("\nData saved to " + filename);
    }

    public void saveToCsv(List<Main.CongressMember> members, String filename) throws IOException {
        StringBuilder csv = new StringBuilder();
        csv.append("name,parliamentary_group,email,profile_url\n");

        for (Main.CongressMember member : members) {
            csv.append(escapeCSV(member.name())).append(",")
                    .append(escapeCSV(member.parliamentaryGroup())).append(",")
                    .append(escapeCSV(member.email())).append(",")
                    .append(escapeCSV(member.profileUrl())).append("\n");
        }

        Files.writeString(Path.of(filename), csv.toString());
        System.out.println("Data saved to " + filename);
    }

    private String escapeCSV(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}


