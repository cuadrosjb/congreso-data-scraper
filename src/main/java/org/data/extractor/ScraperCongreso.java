package org.data.extractor;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class ScraperCongreso {

    private static final String URL_CONGRESISTAS =
            "https://www.congreso.gob.pe/pleno/congresistas/";

    // Datos de conexión a Postgres
    private static final String DB_URL =
            "jdbc:postgresql://localhost:5432/congreso_db";
    private static final String DB_USER = "postgres";
    private static final String DB_PASS = "postgres";

    private record Congresista(
            String nombre,
            String grupoParlamentario,
            String email,
            String periodo,
            String fuente
    ) {}

    public static void main(String[] args) throws Exception {
        // 1. Descargar HTML
        String html = fetchHtml(URL_CONGRESISTAS);

        // 2. Parsear con Jsoup
        List<Congresista> congresistas = parseCongresistas(html);

        // 3. Insertar en Postgres
//        insertIntoPostgres(congresistas);

        System.out.println("Listo. Insertados " + congresistas.size() + " congresistas.");

        congresistas.forEach(System.out::println);


    }

    private static String fetchHtml(String url) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("User-Agent", "Java-HttpClient/25 (data-collection academic)")
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + response.statusCode() + " al pedir " + url);
        }

        return response.body();
    }

    private static List<Congresista> parseCongresistas(String html) {
        Document doc = Jsoup.parse(html);

//        System.out.println(doc);

        List<Congresista> result = new ArrayList<>();

        // Truco: cada congresista aparece como:
        // [Image] [link con el nombre] [texto grupo] [texto email]
        // En el HTML la parte de la lista está hacia las líneas ~163+ :contentReference[oaicite:1]{index=1}
        //
        // Vamos a ubicarlos así:
        // - Encontrar todos los anchors que lleven a "/pleno/congresistas/..."
        // - De ahí, leer los textos hermanos (grupo y correo).
//        Elements links = doc.select("a[href*=\"/pleno/congresistas/\"]");
        Elements links = doc.select("a[href*=\"congresistas\"]");

        String periodoActual = "2021-2026"; // esta página, por defecto, lista el periodo actual

        for (Element a : links) {
            String nombre = a.text().trim();
            if (nombre.isEmpty()) continue;

            // Vamos a leer todos los textos visibles dentro del padre inmediato
            Element parent = a.parent();
            if (parent == null) parent = a;

            List<String> texts = parent
                    .textNodes()
                    .stream()
                    .map(t -> t.text().trim())
                    .filter(s -> !s.isEmpty())
                    .toList();

            // Como parent.text() incluye también el nombre,
            // podemos reconstruir toda la línea y luego extraer grupo/email
            // Otra forma (más robusta) es recorrer los siblings:
            List<String> siblingsText = new ArrayList<>();
            parent.childNodes().forEach(node -> {
                String txt = node.toString().trim();
                // Jsoup maneja esto un poco feo; preferimos text() de Element:
                if (node instanceof Element el) {
                    siblingsText.add(el.text().trim());
                }
            });

            // Pero lo más simple en este caso es: el nombre está en a.text(),
            // y el resto de textos (grupo, email) están en parent.ownText() y siguientes.
            // Hacemos una versión híbrida:

            // a.parent() contiene algo como:
            // "Acuña Peralta María Grimaneza ALIANZA PARA EL PROGRESO gacuna@congreso.gob.pe"
            String fullText = parent.text().trim();
            // fullText -> "Acuña Peralta María Grimaneza ALIANZA PARA EL PROGRESO gacuna@congreso.gob.pe"

            // Quitamos el nombre del inicio:
            String remainder = fullText.replaceFirst("^" + java.util.regex.Pattern.quote(nombre), "")
                    .trim();
            // remainder -> "ALIANZA PARA EL PROGRESO gacuna@congreso.gob.pe"

            String grupo = null;
            String email = null;

            // Como el correo siempre tiene '@', podemos tomar la última "palabra" con '@'
            // y todo lo anterior como grupo.
            String[] parts = remainder.split("\\s+");
            int emailIndex = -1;
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].contains("@")) {
                    emailIndex = i;
                    break;
                }
            }
            if (emailIndex >= 0) {
                email = parts[emailIndex];
                // grupo = todo lo anterior junto
                if (emailIndex > 0) {
                    grupo = String.join(" ",
                            java.util.Arrays.copyOfRange(parts, 0, emailIndex));
                }
            } else {
                // Si por alguna razón no encontramos email, todo el remainder lo tratamos como grupo
                grupo = remainder.isEmpty() ? null : remainder;
            }

            Congresista c = new Congresista(
                    nombre,
                    grupo,
                    email,
                    periodoActual,
                    URL_CONGRESISTAS
            );
            result.add(c);
        }

        return result;
    }

//    private static void insertIntoPostgres(List<Congresista> congresistas) throws SQLException {
//        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
//            conn.setAutoCommit(false);
//
//            String sql = """
//                    INSERT INTO congresistas
//                        (nombre, grupo_parlamentario, email, periodo, fuente)
//                    VALUES (?, ?, ?, ?, ?)
//                    """;
//
//            try (PreparedStatement ps = conn.prepareStatement(sql)) {
//                for (Congresista c : congresistas) {
//                    ps.setString(1, c.nombre());
//                    ps.setString(2, c.grupoParlamentario());
//                    ps.setString(3, c.email());
//                    ps.setString(4, c.periodo());
//                    ps.setString(5, c.fuente());
//                    ps.addBatch();
//                }
//                ps.executeBatch();
//            }
//
//            conn.commit();
//        }
//    }
}