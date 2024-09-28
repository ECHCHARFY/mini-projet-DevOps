import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import com.sun.net.httpserver.*;

public class SimpleServer {
    private static final String DB_URL = "jdbc:postgresql://db:5432/projet";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "root99";

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8083), 0);
        server.createContext("/", new BookHandler());
        server.createContext("/books", new BooksPageHandler());
        server.start();
        System.out.println("Server started on port 8083");
    }

    static class BookHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                // Récupérer les données du formulaire
                InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                BufferedReader reader = new BufferedReader(isr);
                String query = reader.readLine();
                Map<String, String> params = queryToMap(query);

                String reference = params.get("reference");
                String title = params.get("title");
                String domain = params.get("domain");

                // Stocker le livre dans la base de données
                storeBookData(reference, title, domain);

                // Réafficher la page principale avec un message de confirmation
                String response = "<html><body><h1>Book Added Successfully</h1><p>Reference: " + reference + "</p><p>Title: " + title + "</p><p>Domain: " + domain + "</p><a href=\"/\">Add Another Book</a><br><a href=\"/books\">View All Books</a></body></html>";
                exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes(StandardCharsets.UTF_8));
                os.close();
            } else {
                // Afficher le formulaire d'ajout de livre
                serveAddBookPage(exchange);
            }
        }

        // Convertir la requête en map de paramètres
        private Map<String, String> queryToMap(String query) {
            Map<String, String> map = new HashMap<>();
            if (query != null) {
                String[] pairs = query.split("&");
                for (String pair : pairs) {
                    String[] keyValue = pair.split("=");
                    if (keyValue.length == 2) {
                        map.put(URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8), URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8));
                    }
                }
            }
            return map;
        }

        // Stocker les données du livre dans la base de données
        private void storeBookData(String reference, String title, String domain) {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String sql = "INSERT INTO books (reference, title, domain) VALUES (?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, reference);
                    stmt.setString(2, title);
                    stmt.setString(3, domain);
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // Servir la page d'ajout de livre
        private void serveAddBookPage(HttpExchange exchange) throws IOException {
            String response = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Add Book</title>
                    <style>
                        body { font-family: Arial, sans-serif; }
                        .container { max-width: 600px; margin: auto; padding: 20px; }
                        h1 { color: #333; }
                        form { display: flex; flex-direction: column; }
                        label { margin: 10px 0 5px; }
                        input { padding: 10px; margin-bottom: 10px; border: 1px solid #ccc; border-radius: 5px; }
                        input[type="submit"] { background-color: #4CAF50; color: white; border: none; cursor: pointer; }
                        input[type="submit"]:hover { background-color: #45a049; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>Add a Book</h1>
                        <form action="/" method="post">
                            <label for="reference">Reference:</label>
                            <input type="text" id="reference" name="reference" required>
                            <label for="title">Title:</label>
                            <input type="text" id="title" name="title" required>
                            <label for="domain">Domain:</label>
                            <input type="text" id="domain" name="domain" required>
                            <input type="submit" value="Add Book">
                        </form>
                    </div>
                </body>
                </html>
            """;
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes(StandardCharsets.UTF_8));
            os.close();
        }
    }

    static class BooksPageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Récupérer la liste des livres depuis la base de données
            List<Map<String, String>> books = getBooks();

            // Générer le HTML avec la liste des livres
            StringBuilder response = new StringBuilder();
            response.append("""
                <!DOCTYPE html>
                <html>
                <head>
                    <title>All Books</title>
                    <style>
                        body { font-family: Arial, sans-serif; }
                        .container { max-width: 800px; margin: auto; padding: 20px; }
                        h1 { color: #333; }
                        table { width: 100%; border-collapse: collapse; margin-top: 20px; }
                        table, th, td { border: 1px solid #ddd; }
                        th, td { padding: 10px; text-align: left; }
                        th { background-color: #f2f2f2; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>All Books</h1>
                        <table>
                            <tr>
                                <th>Reference</th>
                                <th>Title</th>
                                <th>Domain</th>
                            </tr>
            """);

            for (Map<String, String> book : books) {
                response.append("<tr>")
                        .append("<td>").append(book.get("reference")).append("</td>")
                        .append("<td>").append(book.get("title")).append("</td>")
                        .append("<td>").append(book.get("domain")).append("</td>")
                        .append("</tr>");
            }

            response.append("""
                        </table>
                        <br><a href="/">Add Another Book</a>
                    </div>
                </body>
                </html>
            """);

            exchange.sendResponseHeaders(200, response.toString().getBytes(StandardCharsets.UTF_8).length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.toString().getBytes(StandardCharsets.UTF_8));
            os.close();
        }

        // Récupérer les livres de la base de données
        private List<Map<String, String>> getBooks() {
            List<Map<String, String>> books = new ArrayList<>();
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String sql = "SELECT reference, title, domain FROM books";
                try (Statement stmt = conn.createStatement()) {
                    ResultSet rs = stmt.executeQuery(sql);
                    while (rs.next()) {
                        Map<String, String> book = new HashMap<>();
                        book.put("reference", rs.getString("reference"));
                        book.put("title", rs.getString("title"));
                        book.put("domain", rs.getString("domain"));
                        books.add(book);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return books;
        }
    }
}
