package com.eduaircontrol.mssensor.infrastructure.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento del ciclo de vida de una instalacion de sensor.
 *
 * <p>Contrato publicado hacia {@code eduaircontrol.sensor} y consumido por
 * ms-environment-monitoring, que mantiene con el la proyeccion
 * {@code installation_projection}. Es esa proyeccion la que hace que las
 * mediciones se puedan atribuir a un ambiente: sin ella se guardan pero no se ven.
 *
 * <p>Los tipos comparten sobre y se distinguen por {@code eventType} y la
 * routing key: {@code sensor.installation.created} /
 * {@code sensor.installation.closed} / {@code sensor.installation.deleted}. En el
 * payload la baja se refleja con {@code removedAt}.
 *
 * <p>{@code deleted} es el borrado <b>fisico</b> de la instalacion (purga de datos
 * de prueba o de un alta por error). El consumidor no solo la marca como retirada:
 * borra su fila de {@code installation_projection} y las mediciones que la
 * referenciaban, que sin proyeccion quedarian invisibles pero ocupando espacio.
 *
 * <p><b>El payload no lleva environmentTypeId</b>: este servicio solo valida que el
 * UUID del ambiente tenga forma correcta (DEC-007) y no posee el catalogo de tipos.
 */
public record SensorInstallationEvent(
        UUID eventId,
        String eventType,
        UUID aggregateId,
        String aggregateType,
        Instant occurredAt,
        Integer version,
        Payload payload,
        Metadata metadata) {

    public static final String INSTALLED_TYPE = "SensorInstalled";

    public static final String REMOVED_TYPE = "SensorRemoved";

    public static final String DELETED_TYPE = "SensorInstallationDeleted";

    public static final String AGGREGATE_TYPE = "SensorInstallation";

    public static final int SCHEMA_VERSION = 1;

    public record Payload(
            UUID sensorInstallationId,
            UUID sensorId,
            UUID educationalEnvironmentId,
            Instant installedAt,
            Instant removedAt) {
    }

    public record Metadata(UUID correlationId, UUID causationId, UUID userId) {
    }

    public SensorInstallationEvent {
        if (version == null || version <= 0) {
            version = SCHEMA_VERSION;
        }
    }

    public static SensorInstallationEvent installed(
            UUID sensorInstallationId, UUID sensorId, UUID educationalEnvironmentId,
            Instant installedAt) {
        return new SensorInstallationEvent(
                UUID.randomUUID(), INSTALLED_TYPE, sensorInstallationId, AGGREGATE_TYPE,
                Instant.now(), SCHEMA_VERSION,
                new Payload(sensorInstallationId, sensorId, educationalEnvironmentId,
                        installedAt, null),
                null);
    }

    public static SensorInstallationEvent removed(
            UUID sensorInstallationId, UUID sensorId, UUID educationalEnvironmentId,
            Instant installedAt, Instant removedAt) {
        return new SensorInstallationEvent(
                UUID.randomUUID(), REMOVED_TYPE, sensorInstallationId, AGGREGATE_TYPE,
                Instant.now(), SCHEMA_VERSION,
                new Payload(sensorInstallationId, sensorId, educationalEnvironmentId,
                        installedAt, removedAt),
                null);
    }

    /**
     * Borrado fisico: el consumidor debe purgar, no solo marcar.
     *
     * <p>{@code removedAt} se rellena con el instante del borrado solo para que el
     * payload quede bien formado; quien lo recibe lo ignora y elimina la fila.
     */
    public static SensorInstallationEvent deleted(
            UUID sensorInstallationId, UUID sensorId, UUID educationalEnvironmentId,
            Instant installedAt, Instant removedAt) {
        return new SensorInstallationEvent(
                UUID.randomUUID(), DELETED_TYPE, sensorInstallationId, AGGREGATE_TYPE,
                Instant.now(), SCHEMA_VERSION,
                new Payload(sensorInstallationId, sensorId, educationalEnvironmentId,
                        installedAt, removedAt),
                null);
    }

    /** Routing key que le toca a este evento en el exchange topic. */
    public String routingKey() {
        return switch (eventType) {
            case INSTALLED_TYPE -> "sensor.installation.created";
            case DELETED_TYPE -> "sensor.installation.deleted";
            default -> "sensor.installation.closed";
        };
    }
}
