package com.audit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "audit_events")
@CompoundIndexes({
        @CompoundIndex(name = "service_type", def = "{'serviceName : 1 , 'timestamp : -1}"),
        @CompoundIndex(name = "actor_time", def = "{'actorId' : 1 ,'timestamp : -1 }"),
        @CompoundIndex(name = "type_time", def = "{'eventType' : 1 ,'timestamp : -1 }"),

})
public class AuditEvent {

    @Id
    private String id;

    @Indexed
    private String serviceName;

    @Indexed
    private String eventType;

    @Indexed
    private String actorId;

    private String resourceId;

    private String outcome;

    @Indexed
    private String correlationId;

    @Indexed
    private Instant timestamp;

    private Instant clientTimestamp;

    private String ipAddress;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

}
