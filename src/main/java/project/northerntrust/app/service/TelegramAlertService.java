package project.northerntrust.app.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import project.northerntrust.app.entity.Transfer;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Fire-and-forget Telegram alerts for banking events (holds, approvals,
 * declines, escalations, large settlements). Never blocks or fails the
 * caller — delivery problems are logged and swallowed.
 */
@Service
public class TelegramAlertService {

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

    public void sendRaw(String text) {
        if (!isConfigured()) {
            return;
        }
        CompletableFuture.runAsync(() -> {
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
                    System.out.println("[Telegram Alert] Delivered to chat " + chatId);
                } else {
                    System.out.println("[Telegram Alert] Delivery failed, HTTP " + response.statusCode()
                            + ": " + response.body());
                }
            } catch (Exception e) {
                System.out.println("[Telegram Alert] Send failed: " + e.getMessage());
            }
        });
    }

    /**
     * Formats and delivers a transfer lifecycle alert.
     *
     * @param event       short headline, e.g. "TRANSFER HELD FOR APPROVAL"
     * @param t           the transfer entity
     * @param usdPrincipal USD value moved/reserved (may differ from amount for FX rails); nullable
     * @param note        optional trailing context (reasons, decisions…)
     */
    public void transferEvent(String event, Transfer t, BigDecimal usdPrincipal, String note) {
        if (t == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\uD83C\uDFE6 NORTHERN TRUST \u2014 ").append(event).append("\n\n");
        sb.append("Ref     : ").append(safe(t.getReference())).append('\n');
        String customer = "";
        if (t.getUser() != null) {
            customer = safe(t.getUser().getFirstName()) + " " + safe(t.getUser().getLastName());
        }
        sb.append("Customer: ").append(customer.trim().isEmpty() ? "—" : customer.trim());
        if (t.getUser() != null && t.getUser().getAccountNumber() != null) {
            sb.append(" (").append(t.getUser().getAccountNumber()).append(')');
        }
        sb.append('\n');
        sb.append("Type    : ").append(safe(t.getTransferType() != null ? t.getTransferType().name() : "TRANSFER")).append('\n');
        sb.append("Amount  : ").append(safe(t.getCurrency())).append(' ').append(t.getAmount());
        if (usdPrincipal != null && t.getAmount() != null
                && usdPrincipal.compareTo(t.getAmount()) != 0) {
            sb.append("  (USD ").append(usdPrincipal).append(')');
        }
        sb.append('\n');
        if (t.getCounterpartyName() != null && !t.getCounterpartyName().isBlank()) {
            sb.append("To      : ").append(t.getCounterpartyName()).append('\n');
        }
        if (t.getFromAccount() != null && t.getFromAccount().getDisplayName() != null) {
            sb.append("From    : ").append(t.getFromAccount().getDisplayName()).append('\n');
        }
        if (t.getRiskLevel() != null || t.getRiskScore() != null) {
            sb.append("Risk    : ").append(safe(t.getRiskLevel()))
                    .append(t.getRiskScore() != null ? " (" + t.getRiskScore() + ")" : "")
                    .append('\n');
        }
        if (note != null && !note.isBlank()) {
            sb.append("Note    : ").append(note.trim()).append('\n');
        }
        sendRaw(sb.toString());
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
