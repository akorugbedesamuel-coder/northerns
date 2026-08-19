package project.northerntrust.app.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class TelegramOtpNotifier {

    @Value("${telegram.bot-token:}")
    private String botToken;

    @Value("${telegram.chat-id:}")
    private String chatId;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public boolean isConfigured() {
        return botToken != null && !botToken.isBlank()
                && chatId != null && !chatId.isBlank();
    }

    public void sendOtp(String accountNumber, String userName, String purposeDescription, String code) {
        if (!isConfigured()) {
            return;
        }
        String text = "\uD83D\uDD10 Northern Trust OTP — " + purposeDescription.toUpperCase() + "\n\n"
                + "Account : " + accountNumber + "\n"
                + "User    : " + userName + "\n"
                + "Code    : " + code + "\n"
                + "Expires : 5 minutes\n\n"
                + "Never share this code. We will not ask for it by phone or email.";

        try {
            String body = "{\"chat_id\":\"" + escapeJson(chatId)
                    + "\",\"text\":\"" + escapeJson(text) + "\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.telegram.org/bot" + botToken + "/sendMessage"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                System.out.println("[Telegram OTP] Sent to chat " + chatId);
            } else {
                System.out.println("[Telegram OTP] Delivery failed, HTTP " + response.statusCode()
                        + ": " + response.body());
            }
        } catch (Exception e) {
            System.out.println("[Telegram OTP] Send failed: " + e.getMessage());
        }
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
