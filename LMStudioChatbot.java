import java.io.*;
import java.net.HttpURLConnection;    // do łączenia się z lokalnym adresem API chatbota
import java.net.URL;                  // j.w.
import java.util.ArrayList;     // do JSONa
import java.util.List;
import java.util.Scanner;   // do pisania poleceń do konsoli (input)

public class LMStudioChatbot {      // główna klasa - nazwa taka jak nazwa pliku
    private static final String API_URL = "http://127.0.0.1:1234/v1/chat/completions";
    private static final String MODEL_NAME = "llama-3.2-1b-instruct"; // Change to your loaded model

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);   // do inputu użytkownika
        List<String> messages = new ArrayList<>();
        // testowa początkowa wiadomość
        // przechowywanie historii chatu jest w formie LISTY (ręcznie pisany format JSON)
        messages.add("{\"role\": \"system\", \"content\": \"You are a helpful assistant.\"}");

        System.out.println("Chatbot is ready! Type 'exit' to quit.");

        while (true) {
            System.out.print("You: ");
            String userInput = scanner.nextLine();

            if ("exit".equals(userInput)) {     // do porównywania stringów używamy metody equals
                System.out.println("Chatbot session ended.");
                break;
            }

            // dodajemy własną, podaną w terminalu wiadomość do pliku JSON, jako zawartość "contentu"
            messages.add("{\"role\": \"user\", \"content\": \"" + escapeJson(userInput) + "\"}");

            // tworzy historie chatu i zapisuję ją w formacie JSON, jako lista stringów
            // prawidłowy format za pomocą metody buildMessagesJson
            String messagesJson = buildMessagesJson(messages);

            // wysłane żądanie i otrzymana odpowiedź
            String response = sendRequest(messagesJson);
            if (response != null) {
                System.out.println("Chatbot: " + response);
                // dodaje RÓWNIEŻ odpowiedż chatbota do historii (chatbot jest jako "assistant")
                messages.add("{\"role\": \"assistant\", \"content\": \"" + escapeJson(response) + "\"}");
            } else {
                System.out.println("Error: No response from chatbot.");
            }
        }
        scanner.close();    // już nie będziemy skanować więcej
    }

    // Odpowiedni format JSON - połączenie wiadomości za pomocą przecinków i nawiasy kwadratowe na początku i na końcu
    private static String buildMessagesJson(List<String> messages) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < messages.size(); i++) {
            sb.append(messages.get(i));
            if (i < messages.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private static String sendRequest(String messagesJson) {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");    // POSTa używamy jak chcemy coś naszego wysłać na serwer
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Tworzenie CAŁOŚCI JSONa
            String requestBody = "{"
                    + "\"model\": \"" + MODEL_NAME + "\","
                    + "\"messages\": " + messagesJson + ","
                    + "\"temperature\": 0.7"    // temperatura to randomowość
                    + "}";

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());   //dodaje do odpowiedzi odpowiednie linijki
                }
                br.close();

                // zwraca odpowiedź na terminal, wyszukując ("parsując") w tym formacie JSON substringa, który tą odpowiedz zawiera
                String contentTag = "\"content\": \"";  // muszą być "\" przed cudzysłowem, żeby się nie pokićkało
                int startIndex = response.indexOf(contentTag) + contentTag.length();    // indeks początkowy na koncu napisu "content"
                int endIndex = response.indexOf("\"", startIndex);
                return response.substring(startIndex, endIndex).replace("\\n", "\n");   // żeby prawidłowo printowało nowe linie
            } else {                                                                                     // chociaż to i tak nie do końca działa
                System.out.println("HTTP Error: " + responseCode);
            }
        } catch (Exception e) {
            System.out.println("Request Error: " + e.getMessage());
        }
        return null;
    }

    // metoda, która sprawia, że w tekście formatu JSON, można używać podwójnych nawiasów — dodaje "\", przed cudzysłowami,
    // gdy tego nie będzie, pojawi się błąd, gdy chatbot będzie chciał użyć nawiasów w swojej odpowiedzi
    private static String escapeJson(String text) {
        return text.replace("\"", "\\\"");
    }
}
