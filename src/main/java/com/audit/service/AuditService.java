package com.audit.service;

import com.audit.model.AuditEvent;
import com.audit.model.EventEmitRequest;
import com.audit.producer.AuditEventProducer;
import com.audit.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventProducer producer;
    private final AuditEventRepository eventRepository;
    private final RedisTemplate<String, Object> redisTemplate;


    public AuditEvent emitEvent(EventEmitRequest request, String ipAddress) {

        AuditEvent event = AuditEvent.builder()
                .id(UUID.randomUUID().toString())
                .serviceName(request.getServiceName())
                .eventType(request.getEventType())
                .actorId(request.getActorId())
                .resourceId(request.getResourceId())
                .outcome(request.getOutcome())
                .correlationId(
                        request.getCorrelationId() != null
                                ? request.getCorrelationId()
                                : UUID.randomUUID().toString()
                )
                .timestamp(Instant.now())
                .clientTimestamp(request.getClientTimestamp())
                .ipAddress(ipAddress != null ? ipAddress : request.getIpAddress())
                .metadata(request.getMetadata())
                .build();

        producer.publish(event);
        return event;
    }

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        try {
            Object total = redisTemplate.opsForValue().get("audit:total");
            stats.put("totalEvents", total != null ? total : eventRepository.count());

            Map<String, Long> outcomes = new HashMap<>();

            for (String outcome : List.of("SUCCESS", "FAILURE", "WARNING", "INFO")) {
                Object count = redisTemplate.opsForValue()
                        .get("audit:outcome" + outcome + ":count");
                outcomes.put(outcome, count != null ? ((Number) count).longValue() : 0L);
            }
            stats.put("byOutcome", outcomes);

            long last24h = eventRepository.countByTimestampAfter(
                    Instant.now().minus(24, ChronoUnit.HOURS));
            stats.put("last24Hours", last24h);

            Map<Object, Object> lastSeen = redisTemplate.opsForHash()
                    .entries("audit:service:last_seen");
            stats.put("serviceActivity", lastSeen);

            List<Object> recent = redisTemplate.opsForList()
                    .range("audit:recent", 0, 9);
            stats.put("recentEvents", recent != null ? recent : List.of());


        } catch (Exception e) {
           log.warn("Redis unavailable , falling back to MongoDB : {}",
                   e.getMessage());
           stats.put("totalEvents" , eventRepository.count());
           stats.put("source" , "mongodb-fallback");
        }
        stats.put("generatedAt" , Instant.now());
        return stats;
    }

    public Page<AuditEvent> queryEvents(
           String serviceName,
           String eventType,
           String outcome,
           String actorId,
           String correlationId,
           Instant from,
           Instant to,
           int page,
           int size
    ){
        PageRequest pageable = PageRequest.of(
                page , size  , Sort.by(Sort.Direction.DESC , "timestamp"));

        Instant effectiveFrom = from != null ? from : Instant.EPOCH;
        Instant effectiveTo = to != null ? to : Instant.now().plusSeconds(60);

        if (correlationId != null){
            return eventRepository
                    .findByCorrelationIdOrderByTimestampDesc(correlationId , pageable);
        }
        if (actorId != null){
            return eventRepository.findByActorIdOrderByTimestampDesc(actorId , pageable);
        }
        if (serviceName != null) {
            return eventRepository.findByServiceNameOrderByTimestampDesc(serviceName, pageable);
        }
        if (eventType != null) {
            return eventRepository.findByEventTypeOrderByTimestampDesc(eventType, pageable);
        }
        return eventRepository.findAllByOrderByTimestampDesc(pageable);
    }
    public Optional<AuditEvent> getEventById(String id){
        return eventRepository.findById(id);
    }
}
