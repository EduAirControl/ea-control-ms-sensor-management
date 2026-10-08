package com.eduaircontrol.mssensor.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Si el broker esta caido, el evento no debe perderse: la fila sigue pendiente y se
 * reintenta. Publicar directo desde el servicio perderia el evento en silencio.
 */
class OutboxPublisherFailureTest extends OutboxTestBase {

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private OutboxPublisher publisher;

    @Test
    void failedPublishLeavesTheEventPendingWithTheError() {
        org.mockito.Mockito.doThrow(new AmqpException("broker caido"))
                .when(rabbitTemplate).convertAndSend(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(MessagePostProcessor.class));

        UUID eventId = appendInstalled(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        publisher.publish(eventId);

        OutboxEvent event = repository.findById(eventId).orElseThrow();
        assertThat(event.getPublishedAt()).isNull();
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getLastError()).contains("broker caido");
        assertThat(repository.countByPublishedAtIsNull()).isEqualTo(1);
    }
}