package project.northerntrust.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.northerntrust.app.dto.MessageResponse;
import project.northerntrust.app.service.NotificationService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(defaultValue = "2214578903") String accountNumber) {
        return ResponseEntity.ok(notificationService.listForUser(accountNumber));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> unreadCount(
            @RequestParam(defaultValue = "2214578903") String accountNumber) {
        return ResponseEntity.ok(notificationService.unreadCount(accountNumber));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<MessageResponse> markRead(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "2214578903") String accountNumber) {
        return ResponseEntity.ok(notificationService.markRead(accountNumber, id));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<MessageResponse> markAllRead(
            @RequestParam(defaultValue = "2214578903") String accountNumber) {
        return ResponseEntity.ok(notificationService.markAllRead(accountNumber));
    }
}
