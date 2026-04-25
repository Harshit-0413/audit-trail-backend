package com.audit.config;

import com.audit.model.AuditEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;



@Configuration
public class KafkaConfig {
    @Value("${Audit.kafka.topic}")
    private String topicName;

    @Value("${Kafka.audit.partitions}")
    private int partitions;

    @Value("${audit.kafka.replaced}")
    private int replicas;

    @Bean
    public NewTopic auditEventsTopic(){

        return TopicBuilder.name(topicName)
                .partitions(partitions)
                .replicas(replicas)
                .config("retention.ms" ,
                        String.valueOf(30L*24*60*60*1000))
                .config("compression.type" , "lz4")
                .build();
    }

    @Bean
    public KafkaTemplate<String , AuditEvent> kafkaTemplate(ProducerFactory <String , AuditEvent> producerFactory) {

        return new KafkaTemplate<>(producerFactory);
}
        }
