package com.edusphere.notification;

import com.edusphere.common.CurrentUser;
import com.edusphere.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications")
public class NotificationController {
    private final NotificationService service;

    public NotificationController(NotificationService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "My notifications, newest first")
    public PageResponse<NotificationService.NotificationDto> list(@RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "20") int size) {
        return service.list(CurrentUser.id(), page, size);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Number of unread notifications")
    public Map<String, Long> unread() { return Map.of("count", service.unread(CurrentUser.id())); }

    @PostMapping("/{id}/read")
    @Operation(summary = "Mark one notification read")
    public Map<String, String> read(@PathVariable Long id) {
        service.markRead(CurrentUser.id(), id);
        return Map.of("message", "ok");
    }

    @PostMapping("/read-all")
    @Operation(summary = "Mark all notifications read")
    public Map<String, String> readAll() {
        service.markAllRead(CurrentUser.id());
        return Map.of("message", "ok");
    }
}
