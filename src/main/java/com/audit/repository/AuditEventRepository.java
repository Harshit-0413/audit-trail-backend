package com.audit.repository;

import com.audit.model.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditEventRepository extends MongoRepository<AuditEvent, String> {

    Page<AuditEvent> findServiceNameOrderByTimestampDesc(String serviceName, Pageable pageable);

    Page<AuditEvent> findByActorIdOrderByTimestampDesc(String actorId, Pageable pageable);

    Page<AuditEvent> findCorrelationIdOrderByTimestampDesc(
            String correlationId, Pageable pageable
    );

    @Query
            ("{ $and: [ " +
                    "  { $or: [ { 'serviceName': ?0 }, { ?0: null } ] }, " +
                    "  { $or: [ { 'eventType':   ?1 }, { ?1: null } ] }, " +
                    "  { $or: [ { 'outcome':     ?2 }, { ?2: null } ] }, " +
                    "  { 'timestamp': { $gte: ?3, $lte: ?4 } } " +
                    "] }")
    Page<AuditEvent> findWithFilters(
            String serviceName, String eventType, String outcome,
            Instant from, Instant to, Pageable pageable
    );

    long countByServiceName(String serviceName);

    long countByOutcome(String outcome);

    long countByTimestampAfter(Instant since);

    List<AuditEvent> findTop10ByOrderByTimeStampDesc();
}
