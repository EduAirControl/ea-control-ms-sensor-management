package com.eduaircontrol.mssensor.infrastructure.messaging;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Pendientes en orden de antiguedad. El indice parcial sobre
     * {@code published_at IS NULL} hace que esta consulta no toque las filas ya
     * publicadas, que son la mayoria con el tiempo.
     */
    @Query("select e from OutboxEvent e where e.publishedAt is null order by e.occurredAt asc")
    List<OutboxEvent> findPending(Pageable pageable);

    long countByPublishedAtIsNull();

    long countByPublishedAtIsNullAndOccurredAtBefore(Instant cutoff);
}