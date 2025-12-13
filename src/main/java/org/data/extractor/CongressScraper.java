//import java.io.IOException;
//import java.net.URI;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//import java.time.LocalDateTime;
//import java.util.*;
//
//import org.eclipse.store.storage.embedded.types.EmbeddedStorage;
//import org.eclipse.store.storage.embedded.types.EmbeddedStorageManager;
//import org.jsoup.Jsoup;
//import org.jsoup.nodes.Document;
//import org.jsoup.nodes.Element;
//import org.jsoup.select.Elements;
//
//void main() throws IOException, InterruptedException {
//    var scraper = new CongressScraper();
//
//    try (var storage = scraper.initStorage()) {
//        var root = storage.root();
//
//        // STEP 1: Fetch available periods
//        System.out.println("STEP 1: Fetching available periods...");
//        System.out.println("=".repeat(60));
//
//        var periods = scraper.fetchAvailablePeriods();
//        root.setAvailablePeriods(periods);
//        storage.store(root);
//
//        System.out.println("Found " + periods.size() + " periods:");
//        periods.forEach(p -> System.out.println("  - " + p));
//
//        // STEP 2: For each period, fetch members
//        System.out.println("\n\nSTEP 2: Fetching members for each period...");
//        System.out.println("=".repeat(60));
//
//        for (String period : periods) {
//            scraper.fetchAndSavePeriodMembers(period, root, storage);
//            Thread.sleep(1000); // Be polite to the server
//        }
//
//        // STEP 3: Display results
//        System.out.println("\n\nSTEP 3: Summary");
//        System.out.println("=".repeat(60));
//        scraper.displaySummary(root);
//    }
//}
//
//// Data models
//record CongressMember(
//        String name,
//        String parliamentaryGroup,
//        String email,
//        String profileUrl
//) {}
//
//record PeriodData(
//        String periodName,
//        LocalDateTime fetchedAt,
//        List<CongressMember> members
//) {}
//
//// Root object for EclipseStore
//class CongressDataRoot {
//    private List<String> availablePeriods = new ArrayList<>();
//    private Map<String, PeriodData> periodDataMap = new HashMap<>();
//
//    public List<String> getAvailablePeriods() {
//        return availablePeriods;
//    }
//
//    public void setAvailablePeriods(List<String> periods) {
//        this.availablePeriods = periods;
//    }
//
//    public Map<String, PeriodData> getPeriodDataMap() {
//        return periodDataMap;
//    }
//
//    public void addPeriodData(String period, PeriodData data) {
//        periodDataMap.put(period, data);
//    }
//
//    public boolean hasPeriodData(String period) {
//        return periodDataMap.containsKey(period);
//    }
//}
//
//class CongressScraper {
//    private static final String BASE_URL = "https://www.congreso.gob.pe/pleno/congresistas/";
//    private final HttpClient client = HttpClient.newHttpClient();
//
//    // Initialize EclipseStore
//    public EmbeddedStorageManager initStorage() {
//        var storageManager = EmbeddedStorage.Foundation()
//                .setStorageDirectory("congress-data")
//                .setChannelCount(1)
//                .createEmbeddedStorageManager();
//
//        storageManager.start();
//
//        if (storageManager.root() == null) {
//            storageManager.setRoot(new CongressDataRoot());
//            storageManager.storeRoot();
//        }
//
//        return storageManager;
//    }
//
//    // STEP 1: Fetch available periods from the dropdown
//    public List<String> fetchAvailablePeriods() throws IOException, InterruptedException {
//        HttpRequest request = HttpRequest.newBuilder()
//                .uri(URI.create(BASE_URL))
//                .GET()
//                .build();
//
//        HttpResponse<String> response = client.send(request,
//                HttpResponse.BodyHandlers.ofString());
//
//        Document doc = Jsoup.parse(response.body());
//
//        // Find the period dropdown (select element with name or id)
//        Element periodSelect = doc.selectFirst("select");
//
//        if (periodSelect == null) {
//            System.out.println("Warning: Period dropdown not found, using hardcoded list");
//            return getHardcodedPeriods();
//        }
//
//        List<String> periods = new ArrayList<>();
//        Elements options = periodSelect.select("option");
//
//        for (Element option : options) {
//            String value = option.text().trim();
//            if (!value.isEmpty() && !value.equalsIgnoreCase("Todos")) {
//                periods.add(value);
//            }
//        }
//
//        return periods.isEmpty() ? getHardcodedPeriods() : periods;
//    }
//
//    private List<String> getHardcodedPeriods() {
//        return List.of(
//                "Parlamentario 2021 - 2026",
//                "Parlamentario 2016 - 2021",
//                "Parlamentario 2011 - 2016",
//                "Parlamentario 2006 - 2011",
//                "Parlamentario 2001 - 2006",
//                "Parlamentario 2000 - 2001",
//                "Parlamentario 1995 - 2000",
//                "CCD 1992 -1995"
//        );
//    }
//
//    // STEP 2: Fetch members for a specific period
//    public void fetchAndSavePeriodMembers(
//            String period,
//            CongressDataRoot root,
//            EmbeddedStorageManager storage)
//            throws IOException, InterruptedException {
//
//        // Check if already fetched
//        if (root.hasPeriodData(period)) {
//            var existing = root.getPeriodDataMap().get(period);
//            System.out.println("\n" + period);
//            System.out.println("  Status: Already fetched (" + existing.fetchedAt() + ")");
//            System.out.println("  Members: " + existing.members().size());
//            return;
//        }
//
//        System.out.println("\n" + period);
//        System.out.println("  Status: Fetching...");
//
//        // Fetch members for this period
//        var members = fetchMembersForPeriod(period);
//
//        // Create period data with timestamp
//        var periodData = new PeriodData(
//                period,
//                LocalDateTime.now(),
//                members
//        );
//
//        // Save to EclipseStore
//        root.addPeriodData(period, periodData);
//        storage.store(root.getPeriodDataMap());
//
//        System.out.println("  Members: " + members.size());
//        System.out.println("  Saved: ✓");
//    }
//
//    private List<CongressMember> fetchMembersForPeriod(String period)
//            throws IOException, InterruptedException {
//
//        // Note: The website uses JavaScript/AJAX to filter by period
//        // For now, we fetch the default page (current period)
//        // To fetch historical periods, you would need to:
//        // 1. Use Selenium/Playwright for JavaScript execution
//        // 2. Or reverse-engineer the AJAX calls
//
//        HttpRequest request = HttpRequest.newBuilder()
//                .uri(URI.create(BASE_URL))
//                .GET()
//                .build();
//
//        HttpResponse<String> response = client.send(request,
//                HttpResponse.BodyHandlers.ofString());
//
//        Document doc = Jsoup.parse(response.body());
//        Element table = doc.selectFirst("table");
//
//        if (table == null) {
//            return List.of();
//        }
//
//        List<CongressMember> members = new ArrayList<>();
//        Elements rows = table.select("tr");
//
//        // Skip header row
//        for (int i = 1; i < rows.size(); i++) {
//            Element row = rows.get(i);
//            Elements cols = row.select("td");
//
//            if (cols.size() >= 3) {
//                Element nameLink = cols.get(1).selectFirst("a");
//                if (nameLink == null) continue;
//
//                String name = nameLink.text().trim();
//                String profileUrl = BASE_URL + nameLink.attr("href");
//                String group = cols.get(2).text().trim();
//
//                Element emailLink = cols.get(3).selectFirst("a");
//                String email = emailLink != null ? emailLink.text().trim() : "";
//
//                members.add(new CongressMember(name, group, email, profileUrl));
//            }
//        }
//
//        return members;
//    }
//
//    // STEP 3: Display summary
//    public void displaySummary(CongressDataRoot root) {
//        System.out.println("\nTotal periods available: " + root.getAvailablePeriods().size());
//        System.out.println("Total periods fetched: " + root.getPeriodDataMap().size());
//        System.out.println("\nStorage location: congress-data/");
//
//        int totalMembers = root.getPeriodDataMap().values().stream()
//                .mapToInt(p -> p.members().size())
//                .sum();
//        System.out.println("Total members stored: " + totalMembers);
//
//        System.out.println("\nDetailed breakdown:");
//        root.getPeriodDataMap().forEach((period, data) -> {
//            System.out.println("  " + period + ": " +
//                    data.members().size() + " members (fetched: " +
//                    data.fetchedAt() + ")");
//        });
//    }
//}