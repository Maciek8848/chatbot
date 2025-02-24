import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class LMStudioChatbot {
    private static final String API_URL = "http://127.0.0.1:1234/v1/chat/completions";
    private static final String MODEL_NAME = "llama-3.2-1b-instruct"; // Zmień na swój model
    private static final int TIMEOUT = 10000; // 10 sekund timeout
    private static final int MAX_HISTORY = 5; // Ogranicz historię chatu do 5 wiadomości

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        List<String> messages = new ArrayList<>();
        messages.add("{\"role\": \"system\", \"content\": \"You are a helpful assistant.\"}");

        System.out.println("Chatbot is ready! Type 'exit' to quit.");

        while (true) {
            System.out.print("You: ");
            String userInput = scanner.nextLine();

            if ("exit".equalsIgnoreCase(userInput)) {
                System.out.println("Chatbot session ended.");
                break;
            }

            messages.add(String.format("{\"role\": \"user\", \"content\": \"%s\"}", escapeJson(userInput)));

            // Ogranicz historię chatu
            if (messages.size() > MAX_HISTORY) {
                messages = messages.subList(messages.size() - MAX_HISTORY, messages.size());
            }

            String response = sendRequest(messages);
            if (response != null) {
                System.out.println("Chatbot: " + response);
                messages.add(String.format("{\"role\": \"assistant\", \"content\": \"%s\"}", escapeJson(response)));
            } else {
                System.out.println("Error: No response from chatbot. Resetting chat history...");
                messages.clear();
                messages.add("{\"role\": \"system\", \"content\": \"You are a helpful assistant.\"}");
            }
        }
        scanner.close();
    }

    private static String sendRequest(List<String> messages) {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);
            connection.setDoOutput(true);

            String messagesJson = "[" + String.join(",", messages) + "]";
            String requestBody = String.format(
                "{\"model\": \"%s\", \"messages\": %s, \"temperature\": 0.7, \"max_tokens\": 1000}",
                MODEL_NAME, messagesJson
            );

            try (OutputStream os = connection.getOutputStream()) {
                os.write(requestBody.getBytes("utf-8"));
            }

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                    // Parsowanie odpowiedzi JSON
                    String contentTag = "\"content\": \"";
                    int startIndex = response.indexOf(contentTag) + contentTag.length();
                    int endIndex = response.indexOf("\"", startIndex);
                    return response.substring(startIndex, endIndex).replace("\\n", "\n");
                }
            } else {
                System.out.println("HTTP Error: " + connection.getResponseCode());
            }
        } catch (Exception e) {
            System.out.println("Request Error: " + e.getMessage());
        }
        return null;
    }

    private static String escapeJson(String text) {
        return text.replace("\"", "\\\"");
    }
}