package com.audit.controller;

import com.audit.model.AuditEvent;
import com.audit.model.EventEmitRequest;
import com.audit.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor

public class AuditController {

    private final AuditService auditService;

    @PostMapping("/events")
    public ResponseEntity<Map<String, Object>> emitEvent(
            @Valid @RequestBody EventEmitRequest request,
            HttpServletRequest httpRequest
    ) {

        String clientIp = extractClientIp(httpRequest);
        AuditEvent event = auditService.emitEvent(request, clientIp);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "status", "accepted",
                "eventId", event.getId(),
                "correlationId", event.getCorrelationId(),
                "timestamp", event.getTimestamp()
        ));
    }

    @PostMapping("/events/batch")
    public ResponseEntity<Map<String, Object>> emitBatch(
            @Valid @RequestBody List<EventEmitRequest> requests,
            HttpServletRequest httpRequest
    ) {
        if (requests.size() > 100) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Batch size cannot exceed 100 events"));
        }

        String clientIp = extractClientIp(httpRequest);
        List<String> eventIds = requests.stream()
                .map(req -> auditService.emitEvent(req, clientIp).getId())
                .toList();

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "status", "accepted",
                "count", eventIds.size(),
                "eventIds", eventIds
        ));
    }

    @GetMapping("/events")
    public ResponseEntity<Page<AuditEvent>> queryEvents(
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String outcome,
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String correlationId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (size > 100) size = 100;

        Page<AuditEvent> events = auditService.queryEvents(
                serviceName, eventType, outcome, actorId, correlationId, from, to, page, size);

               return ResponseEntity.ok(events);
    }

    @GetMapping("/events/{id}")

    public ResponseEntity<AuditEvent> getEvent(@PathVariable String id){
        return auditService.getEventById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/dashboard/stats")
    public ResponseEntity<Map<String , Object>> getDashboardStats(){
        return ResponseEntity.ok(auditService.getDashboardStats());
    }

    private String extractClientIp (HttpServletRequest request){
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()){
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return realIp != null ? realIp : request.getRemoteAddr();
    }
}

