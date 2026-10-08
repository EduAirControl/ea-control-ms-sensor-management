package com.eduaircontrol.mssensor.infrastructure.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declara el exchange de eventos del dominio sensors. Es <b>topic</b> y no direct:
 * asi ms-environment-monitoring se suscribe a {@code sensor.installation.*} y,
 * mas adelante, otro servicio a {@code sensor.sensor.*}, sin que el productor
 * cambie.
 *
 * <p>Durable porque el broker sobrevive reinicios y los eventos pendientes en la
 * tabla outbox deben reencontrar su cola al volver.
 */
@Configuration
@ConditionalOnProperty(prefix = "app.outbox", name = "enabled", havingValue = "true",
        matchIfMissing = true)
public class OutboxMessagingConfig {

    @Bean
    public TopicExchange sensorEventsExchange(
            @Value("${app.outbox.exchange:eduaircontrol.sensor}") String name) {
        return new TopicExchange(name, true, false);
    }
}