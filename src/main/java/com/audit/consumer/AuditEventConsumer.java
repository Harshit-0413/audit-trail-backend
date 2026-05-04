package com.audit.consumer;

import com.audit.model.AuditEvent;
import com.audit.repository.AuditEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.kafka.support.Acknowledgment;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventConsumer {

    private final AuditEventRepository eventRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${audit.redis.stats-ttl-seconds}")
    private long statsTtlSeconds;

    @Value("${audit.redis.recent-events-size}")
    private long recentEventsSize;

    @KafkaListener(
            topics = "${audit.kafka.topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )

    public void consume(
            ConsumerRecord<String, AuditEvent> record,
            Acknowledgment ack,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {

        AuditEvent event = record.value();
        log.debug("Consuming event [service={} , type={} , partition={} , offset={}]",
                event.getServiceName(), event.getEventType(), partition, offset
        );

        try {
            AuditEvent saved = eventRepository.save(event);
            log.info("Persisted event [id={} , service = {} , type={}]",
                    saved.getId(),
                    saved.getServiceName(),
                    saved.getEventType());

            updateRedisCache(saved);

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process event [service={} , type={}]: {}",
                    event.getServiceName(),
                    event.getEventType(),
                    e.getMessage(), e);
        }
    }

    private void updateRedisCache(AuditEvent event) {
        try {
            redisTemplate.opsForValue()
                    .increment("audit:total");

            redisTemplate.opsForValue()
                            .increment("audit:service"+ event.getServiceName() + ":count");
            if (event.getOutcome() != null){
                redisTemplate.opsForValue()
                        .increment("audit:outcome" + event.getOutcome() + ":count");
            }

        redisTemplate.opsForValue()
                    .increment("audit:eventtype" + event.getEventType() + ":count");

            String hour = event.getTimestamp()
                    .toString().substring(0,13).replace("T" , "-");
            redisTemplate.opsForValue().increment("audit:hourly:" + hour);
            redisTemplate.expire("audit:hourly" + hour , Duration.ofDays(7));

            String json = objectMapper.writeValueAsString(event);
            redisTemplate.opsForList().leftPush("audit:recent" , json);
            redisTemplate.opsForList()
                    .trim("audit:recent" , 0 , recentEventsSize - 1);

            redisTemplate.opsForHash().put(
                    "audit:service:last_seen",
                    event.getServiceName(),
                    event.getTimestamp().toString()
            );
        } catch (Exception e){
            log.warn("Failed to update Redis cache [id={}]: {}",
            event.getId() , e.getMessage());
        }
    }
}
