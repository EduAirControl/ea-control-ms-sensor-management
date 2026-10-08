package com.eduaircontrol.mssensor.infrastructure.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Contrato del evento {@code SensorInstalled}, segun
 * {@code 09-microservices/services/04-ms-sensor-management/events.md}.
 *
 * <p>Usa el sobre estandar del dominio, igual que {@code EnvironmentalDataRecorded}.
 * El {@code eventId} viaja en el cuerpo y no solo como metadato del broker porque
 * es lo que garantiza la idempotencia del consumidor.
 *
 * <p>El payload <b>no</b> lleva {@code environmentTypeId}: este servicio no posee
 * ese dato, solo valida que el UUID del ambiente tenga forma correcta (DEC-007). Quien
 * lo necesita, ms-environment-monitoring, lo resuelve contra su replica local de
 * ambientes. Por eso {@code installation_projection.environment_type_id} es nullable.
 */
public record SensorInstalled(
        UUID eventId,
        String eventType,
        UUID aggregateId,
        String aggregateType,
        Instant occurredAt,
        Integer version,
        Payload payload,
        Metadata metadata) {

    public static final String TYPE = "SensorInstalled";

    public static final String AGGREGATE_TYPE = "SensorInstallation";

    public static final int SCHEMA_VERSION = 1;

    public static final String ROUTING_KEY = "sensor.installation.created";

    public record Payload(
            UUID sensorInstallationId,
            UUID sensorId,
            UUID educationalEnvironmentId,
            Instant installedAt) {
    }

    public record Metadata(UUID correlationId, UUID causationId, UUID userId) {

        public static Metadata empty() {
            return new Metadata(null, null, null);
        }
    }

    /**
     * Normaliza la version del esquema. Se declara como {@code Integer} y no
     * {@code int} a proposito: con un primitivo, un sobre sin {@code version} falla
     * al deserializar antes de llegar aqui.
     */
    public SensorInstalled {
        if (version == null || version <= 0) {
            version = SCHEMA_VERSION;
        }
    }

    /** Construye el evento con sobre completo y metadatos vacios. */
    public static SensorInstalled of(UUID sensorInstallationId, UUID sensorId,
                                     UUID educationalEnvironmentId, Instant installedAt,
                                     Instant occurredAt) {
        return new SensorInstalled(
                UUID.randomUUID(),
                TYPE,
                sensorInstallationId,
                AGGREGATE_TYPE,
                occurredAt,
                SCHEMA_VERSION,
                new Payload(sensorInstallationId, sensorId, educationalEnvironmentId, installedAt),
                Metadata.empty());
    }
}