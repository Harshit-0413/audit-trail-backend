package com.audit.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Data
public class EventEmitRequest {

    @NotBlank(message = "serviceName is required")
    private String serviceName;

    @NotBlank(message = "eventType is required")
    private String eventType;

    private String actorId;
    private String resourceId;

    @Pattern(
            regexp = "SUCCESS|FAILURE|WARNING|INFO",
            message = "outcome must be SUCCESS/FAILURE/WARNING/INFO"
    )
    private String outcome = "INFO";

    private String correlationId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant clientTimestamp;

    private String ipAddress;

    private Map<String, Object> metadata = new HashMap<>();

}
