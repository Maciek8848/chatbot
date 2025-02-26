import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class LMStudioChatbot {
    private static final String API_URL = "http://127.0.0.1:1234/v1/chat/completions";
    private static final String MODEL_NAME = "lmstudio-community/Qwen2.5-7B-Instruct-1M-GGUF"; // Zmień na swój model
    private static final int TIMEOUT = 10000; // 10 sekund timeout

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Chatbot is ready! Type 'exit' to quit.");

        while (true) {
            System.out.print("You: ");
            String userInput = scanner.nextLine();

            if ("exit".equalsIgnoreCase(userInput)) {
                System.out.println("Chatbot session ended.");
                break;
            }

            String response = sendRequest(userInput);
            if (response != null) {
                System.out.println("Chatbot: " + response);
            } else {
                System.out.println("Error: No response from chatbot.");
            }
        }
        scanner.close();
    }

    private static String sendRequest(String userInput) {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);
            connection.setDoOutput(true);

            String requestBody = String.format(
                "{\"model\": \"%s\", \"messages\": [{\"role\": \"user\", \"content\": \"%s\"}], \"temperature\": 0.7, \"max_tokens\": 1000}",
                MODEL_NAME, escapeJson(userInput)
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