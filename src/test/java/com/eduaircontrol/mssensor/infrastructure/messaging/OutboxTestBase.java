package com.eduaircontrol.mssensor.infrastructure.messaging;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;

/**
 * Base de los tests del outbox: H2 para la persistencia y RabbitMQ real.
 *
 * <p>El relay solo se puede verificar de verdad contra un broker de verdad.
 * Simular {@code RabbitTemplate} no probaria ni el exchange, ni la routing key,
 * ni el sobre canonico, que son justo las tres cosas que hay que asegurar.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class OutboxTestBase {

    protected static final RabbitMQContainer RABBIT =
            new RabbitMQContainer("rabbitmq:3.13-alpine");

    protected static final String EXCHANGE = "eduaircontrol.sensor.test";
    protected static final String QUEUE = EXCHANGE + ".test-consumer";

    @Autowired
    protected OutboxRepository repository;

    @Autowired
    protected OutboxWriter writer;

    @Autowired
    protected OutboxRelay relay;

    @Autowired
    protected RabbitTemplate rabbitTemplate;

    @Autowired
    protected ConnectionFactory connectionFactory;

    @BeforeAll
    static void startRabbit() {
        if (!RABBIT.isRunning()) {
            RABBIT.start();
        }
    }

    @DynamicPropertySource
    static void rabbit(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
        registry.add("app.outbox.exchange", () -> EXCHANGE);
        // El relay se dispara de forma explicita en cada test. Con el periodo real
        // entraria en carrera con las aserciones sobre eventos pendientes.
        registry.add("app.outbox.relay-interval-ms", () -> "3600000");
    }

    /**
     * Declara exchange, cola y binding, y purga la cola. El binding cubre
     * {@code sensor.installation.*} a proposito: si el productor cambiara la
     * routing key, estos tests dejarian de recibir el mensaje y fallarian.
     */
    @BeforeEach
    void declareTopology() {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        TopicExchange exchange = new TopicExchange(EXCHANGE, true, false);
        Queue queue = QueueBuilder.durable(QUEUE).build();
        admin.declareExchange(exchange);
        admin.declareQueue(queue);
        admin.declareBinding(BindingBuilder.bind(queue).to(exchange).with("sensor.installation.*"));
        admin.purgeQueue(QUEUE);
    }

    /** La base es compartida por toda la suite: sin vaciar, los conteos no significan nada. */
    @BeforeEach
    void clearOutbox() {
        repository.deleteAll();
    }

    protected UUID appendInstalled(UUID installationId, UUID sensorId, UUID environmentId) {
        Instant installedAt = Instant.now().minusSeconds(60);
        SensorInstalled event =
                SensorInstalled.of(installationId, sensorId, environmentId, installedAt, installedAt);
        return writer.append(
                event.eventType(),
                event.aggregateType(),
                event.aggregateId().toString(),
                SensorInstalled.ROUTING_KEY,
                event,
                event.occurredAt());
    }

    protected String bodyOf() {
        var received = rabbitTemplate.receive(QUEUE, 3000);
        return received == null ? null
                : new String(received.getBody(), java.nio.charset.StandardCharsets.UTF_8);
    }
}