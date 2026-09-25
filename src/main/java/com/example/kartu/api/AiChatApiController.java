package com.example.kartu.api;

import com.example.kartu.models.User;
import com.example.kartu.services.AiChatService;
import com.example.kartu.services.OpenRouterClient;
import com.example.kartu.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.function.Function;

/** SKPL-F20 (usulan): AI chat sessions for admins. /api/admin/** is ROLE_ADMIN only (SecurityConfig). */
@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
public class AiChatApiController {

    private final AiChatService aiChatService;
    private final OpenRouterClient openRouterClient;
    private final UserService userService;

    @Transactional(readOnly = true)
    @GetMapping("/sessions")
    public ResponseEntity<?> listSessions(Authentication auth) {
        return withAdmin(auth, admin -> ResponseEntity.ok(aiChatService.listSessions(admin)));
    }

    @PostMapping("/sessions")
    public ResponseEntity<?> createSession(Authentication auth) {
        return withAdmin(auth, admin -> ResponseEntity.ok(aiChatService.createSession(admin)));
    }

    @Transactional(readOnly = true)
    @GetMapping("/sessions/{id}/messages")
    public ResponseEntity<?> messages(@PathVariable Long id, Authentication auth) {
        return withAdmin(auth, admin -> ResponseEntity.ok(aiChatService.messages(admin, id)));
    }

    @PostMapping("/sessions/{id}/messages")
    public ResponseEntity<?> send(@PathVariable Long id, @RequestBody Map<String, Object> body, Authentication auth) {
        String prompt = body.get("prompt") instanceof String s ? s : null;
        return withAdmin(auth, admin -> ResponseEntity.ok(aiChatService.sendMessage(admin, id, prompt)));
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<?> deleteSession(@PathVariable Long id, Authentication auth) {
        return withAdmin(auth, admin -> {
            aiChatService.deleteSession(admin, id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Sesi dihapus."));
        });
    }

    @DeleteMapping("/sessions")
    public ResponseEntity<?> deleteAllSessions(Authentication auth) {
        return withAdmin(auth, admin -> {
            aiChatService.deleteAllSessions(admin);
            return ResponseEntity.ok(Map.of("success", true, "message", "Semua sesi dihapus."));
        });
    }

    @GetMapping("/usage")
    public ResponseEntity<?> usage() {
        return ResponseEntity.ok(openRouterClient.usage());
    }

    private ResponseEntity<?> withAdmin(Authentication auth, Function<User, ResponseEntity<?>> action) {
        User admin = userService.findCurrentUser(auth).orElse(null);
        if (admin == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Silakan login terlebih dahulu."));
        }
        try {
            return action.apply(admin);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", String.valueOf(e.getReason())));
        }
    }
}
