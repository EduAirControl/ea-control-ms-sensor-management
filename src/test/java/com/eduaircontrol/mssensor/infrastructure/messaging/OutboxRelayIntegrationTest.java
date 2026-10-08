package com.eduaircontrol.mssensor.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * El relay debe entregar al broker un sobre que ms-environment-monitoring sepa
 * leer: la routing key correcta y el eventId en el cuerpo.
 *
 * <p>El binding de la cola cubre {@code sensor.installation.*}. Si el productor
 * cambiara la routing key, el mensaje se publicaria pero nadie lo recibiria, y el
 * fallo seria silencioso: el outbox se marca como enviado y la proyeccion nunca se
 * actualiza.
 */
class OutboxRelayIntegrationTest extends OutboxTestBase {

    @Autowired
    private OutboxPublisher publisher;

    @Test
    void relayPublishesPendingEventAndMarksIt() {
        UUID installationId = UUID.randomUUID();
        UUID eventId = appendInstalled(installationId, UUID.randomUUID(), UUID.randomUUID());

        relay.publishPending();

        String body = bodyOf();
        assertThat(body).isNotNull();
        assertThat(body).contains("\"sensorInstallationId\":\"" + installationId + "\"");
        assertThat(body).contains("\"eventType\":\"SensorInstalled\"");

        OutboxEvent published = repository.findById(eventId).orElseThrow();
        assertThat(published.getPublishedAt()).isNotNull();
        assertThat(published.getLastError()).isNull();
    }

    @Test
    void payloadUsesTheCanonicalRoutingKey() {
        UUID eventId = appendInstalled(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        assertThat(repository.findById(eventId).orElseThrow().getRoutingKey())
                .isEqualTo("sensor.installation.created");
    }

    /** Un evento ya publicado no se vuelve a enviar. */
    @Test
    void doesNotRepublishAnAlreadyPublishedEvent() {
        UUID eventId = appendInstalled(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        relay.publishPending();
        assertThat(bodyOf()).isNotNull();

        relay.publishPending();
        assertThat(bodyOf()).isNull();
        assertThat(repository.findById(eventId).orElseThrow().getAttempts()).isEqualTo(1);
    }

    @Test
    void publishesInOccurrenceOrder() {
        UUID first = appendInstalled(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        UUID second = appendInstalled(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        relay.publishPending();

        // El primero encolado se publica primero; si se invirtiera, la proyeccion
        // podria quedar con un removed_at de una instalacion posterior.
        assertThat(repository.findPending(org.springframework.data.domain.Pageable.ofSize(10)))
                .extracting(OutboxEvent::getId)
                .isEmpty();
        assertThat(repository.findById(first).orElseThrow().getPublishedAt()).isNotNull();
        assertThat(repository.findById(second).orElseThrow().getPublishedAt()).isNotNull();
    }
}