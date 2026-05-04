package com.audit.producer;


import com.audit.model.AuditEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventProducer {

    private final KafkaTemplate<String, AuditEvent> kafkaTemplate;

    @Value("${audit.kafka.topic}")
    private String topicName;


    public CompletableFuture<SendResult<String, AuditEvent>> publish(
            AuditEvent event
    ) {
        String partitionKey = event.getServiceName();

        CompletableFuture<SendResult<String, AuditEvent>> future = kafkaTemplate.send(topicName, partitionKey, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish event [id = {} , service={}]: {}",
                        event.getId(),
                        event.getServiceName(),
                        ex.getMessage());
            } else {
                log.debug("Published event [id={} , parition={} , offset={}]",
                        event.getId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
        return future;
    }
}


