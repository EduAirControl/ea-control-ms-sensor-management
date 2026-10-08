package com.eduaircontrol.mssensor.infrastructure.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Contrato del evento {@code SensorRemoved}, segun
 * {@code 09-microservices/services/04-ms-sensor-management/events.md}.
 *
 * <p>Es la contraparte de {@link SensorInstalled} y comparte su sobre. El
 * consumidor lo aplica con el mismo upsert: informar {@code removedAt} es lo que
 * hace que las consultas con {@code p.removed_at is null} dejen de contar esa
 * instalacion, sin borrar la fila (que sigue siendo historico valido).
 */
public record SensorRemoved(
        UUID eventId,
        String eventType,
        UUID aggregateId,
        String aggregateType,
        Instant occurredAt,
        Integer version,
        Payload payload,
        Metadata metadata) {

    public static final String TYPE = "SensorRemoved";

    public static final String AGGREGATE_TYPE = "SensorInstallation";

    public static final int SCHEMA_VERSION = 1;

    public static final String ROUTING_KEY = "sensor.installation.closed";

    public record Payload(
            UUID sensorInstallationId,
            UUID sensorId,
            UUID educationalEnvironmentId,
            Instant installedAt,
            Instant removedAt) {
    }

    public record Metadata(UUID correlationId, UUID causationId, UUID userId) {

        public static Metadata empty() {
            return new Metadata(null, null, null);
        }
    }

    public SensorRemoved {
        if (version == null || version <= 0) {
            version = SCHEMA_VERSION;
        }
    }

    public static SensorRemoved of(UUID sensorInstallationId, UUID sensorId,
                                   UUID educationalEnvironmentId, Instant installedAt,
                                   Instant removedAt, Instant occurredAt) {
        return new SensorRemoved(
                UUID.randomUUID(),
                TYPE,
                sensorInstallationId,
                AGGREGATE_TYPE,
                occurredAt,
                SCHEMA_VERSION,
                new Payload(sensorInstallationId, sensorId, educationalEnvironmentId, installedAt,
                        removedAt),
                Metadata.empty());
    }
}