import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class MCQServer {
    private static final String WEB_ROOT = "d:/qp-shuffler/web";

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new StaticHandler());
        server.createContext("/upload", new UploadHandler());
        server.setExecutor(null);
        System.out.println("MCQ QP Shuffler running at http://localhost:8080/");
        server.start();
    }

    static class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) path = "/index.html";
            File file = new File(WEB_ROOT + path.replace("..", ""));
            if (file.exists() && file.isFile()) {
                String contentType = guessType(file.getName());
                exchange.getResponseHeaders().set("Content-Type", contentType);
                byte[] bytes = Files.readAllBytes(file.toPath());
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            } else {
                byte[] msg = "404 Not Found".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, msg.length);
                exchange.getResponseBody().write(msg);
            }
            exchange.close();
        }

        private String guessType(String filename) {
            if (filename.endsWith(".html")) return "text/html; charset=utf-8";
            if (filename.endsWith(".css")) return "text/css; charset=utf-8";
            if (filename.endsWith(".js")) return "application/javascript; charset=utf-8";
            return "application/octet-stream";
        }
    }

    static class UploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            Headers headers = exchange.getRequestHeaders();
            String contentType = headers.getFirst("Content-Type");
            String boundary = null;
            if (contentType != null && contentType.contains("boundary=")) {
                boundary = "--" + contentType.split("boundary=")[1];
            }
            if (boundary == null) {
                sendText(exchange, 400, "Invalid multipart request");
                return;
            }

            byte[] allBytes = readAllBytes(exchange.getRequestBody());
            String raw = new String(allBytes, StandardCharsets.ISO_8859_1);

            Map<String, String> fields = parseMultipart(raw, boundary);

            String fileContent = fields.get("questionsFile");
            String setsValue = fields.getOrDefault("sets", "3");
            int sets;
            try { sets = Integer.parseInt(setsValue); } catch (NumberFormatException e) { sets = 3; }

            if (fileContent == null || fileContent.isEmpty()) {
                sendText(exchange, 400, "No question file uploaded.");
                return;
            }

            List<Question> questions = MCQShuffler.parseQuestions(fileContent);
            if (questions.isEmpty()) {
                sendText(exchange, 400, "No valid questions found in upload. Use Q|A|B|C|D|Correct format.");
                return;
            }

            List<List<Question>> generated = MCQShuffler.generateSets(questions, sets);
            String output = MCQShuffler.createOutputText(generated);

            byte[] data = output.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
            exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=shuffled-sets.txt");
            exchange.sendResponseHeaders(200, data.length);
            exchange.getResponseBody().write(data);
            exchange.close();
        }

        private void sendText(HttpExchange exchange, int code, String text) throws IOException {
            byte[] data = text.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(code, data.length);
            exchange.getResponseBody().write(data);
            exchange.close();
        }

        private byte[] readAllBytes(InputStream input) throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int len;
            while ((len = input.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            return baos.toByteArray();
        }

        private Map<String,String> parseMultipart(String raw, String boundary) {
            Map<String, String> map = new HashMap<>();
            String[] parts = raw.split(boundary);
            for (String part : parts) {
                part = part.trim();
                if (part.isEmpty() || part.equals("--")) continue;
                int headerEnd = part.indexOf("\r\n\r\n");
                if (headerEnd < 0) continue;
                String header = part.substring(0, headerEnd);
                String body = part.substring(headerEnd + 4);

                String name = null;
                if (header.contains("Content-Disposition")) {
                    String[] headerLines = header.split("\r\n");
                    for (String h : headerLines) {
                        if (h.toLowerCase().contains("content-disposition")) {
                            for (String token : h.split(";")) {
                                token = token.trim();
                                if (token.startsWith("name=")) {
                                    name = token.substring(5).trim().replaceAll("\"", "");
                                }
                            }
                        }
                    }
                }
                if (name != null) {
                    if (body.endsWith("\r\n")) {
                        body = body.substring(0, body.length()-2);
                    }
                    map.put(name, body);
                }
            }
            return map;
        }
    }
}