package com.eduaircontrol.mssensor.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eduaircontrol.mssensor.domain.port.out.SensorInstallationRepository;
import com.eduaircontrol.mssensor.domain.port.out.SensorRepository;
import com.eduaircontrol.mssensor.infrastructure.messaging.OutboxWriter;
import com.eduaircontrol.mssensor.infrastructure.messaging.SensorInstalled;
import com.eduaircontrol.mssensor.infrastructure.messaging.SensorRemoved;
import com.eduaircontrol.mssensor.shared.exception.ConflictException;
import com.eduaircontrol.mssensor.shared.exception.NotFoundException;
import com.eduaircontrol.mssensor.shared.exception.ValidationException;
import com.eduaircontrol.mssensor.domain.model.Sensor;
import com.eduaircontrol.mssensor.domain.model.SensorInstallation;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SensorInstallationServiceTest {

    private SensorInstallationRepository installationRepository;
    private SensorRepository sensorRepository;
    private OutboxWriter outboxWriter;
    private SensorInstallationService installationService;

    @BeforeEach
    void setUp() {
        installationRepository = mock(SensorInstallationRepository.class);
        sensorRepository = mock(SensorRepository.class);
        outboxWriter = mock(OutboxWriter.class);
        installationService = new SensorInstallationService(
                installationRepository, sensorRepository, outboxWriter);
    }

    @Test
    void createRejectsWhenSensorDoesNotExist() {
        UUID sensorId = UUID.randomUUID();
        when(sensorRepository.findById(sensorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> installationService.create(
                sensorId, UUID.randomUUID(), Instant.now()))
                .isInstanceOf(NotFoundException.class);

        verify(installationRepository, never()).save(any());
        verify(outboxWriter, never()).append(anyString(), anyString(), anyString(),
                anyString(), any(), any());
    }

    @Test
    void createRejectsOverlappingInstallation() {
        UUID sensorId = UUID.randomUUID();
        when(sensorRepository.findById(sensorId)).thenReturn(
                Optional.of(Sensor.builder().id(sensorId).serialNumber("SN-1")
                        .sensorModelId(UUID.randomUUID()).sensorStatusId(UUID.randomUUID()).build()));
        when(installationRepository.existsOverlapForSensor(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> installationService.create(
                sensorId, UUID.randomUUID(), Instant.now()))
                .isInstanceOf(ConflictException.class);

        verify(installationRepository, never()).save(any());
        verify(outboxWriter, never()).append(anyString(), anyString(), anyString(),
                anyString(), any(), any());
    }

    @Test
    void createPersistsOpenInstallation() {
        UUID sensorId = UUID.randomUUID();
        Instant installedAt = Instant.now().minus(1, ChronoUnit.DAYS);
        when(sensorRepository.findById(sensorId)).thenReturn(
                Optional.of(Sensor.builder().id(sensorId).serialNumber("SN-1")
                        .sensorModelId(UUID.randomUUID()).sensorStatusId(UUID.randomUUID()).build()));
        when(installationRepository.existsOverlapForSensor(sensorId, installedAt)).thenReturn(false);
        // El id lo genera JPA al guardar (@GeneratedValue UUID); el mock debe
        // reflejarlo porque el servicio lo usa para el routing del evento.
        when(installationRepository.save(any())).thenAnswer(invocation -> {
            SensorInstallation saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        SensorInstallation created = installationService.create(
                sensorId, UUID.randomUUID(), installedAt);

        assertThat(created.getInstalledAt()).isEqualTo(installedAt);
        assertThat(created.isActive()).isTrue();
    }

    /**
     * El alta de una instalacion debe encolar SensorInstalled (ADR-007). Si esto
     * falla, ms-environment-monitoring nunca proyecta la instalacion y las
     * mediciones de esa aula quedan invisibles aunque se guarden.
     */
    @Test
    void createEnqueuesSensorInstalled() {
        UUID sensorId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();
        Instant installedAt = Instant.now().minus(1, ChronoUnit.DAYS);
        when(sensorRepository.findById(sensorId)).thenReturn(
                Optional.of(Sensor.builder().id(sensorId).serialNumber("SN-1")
                        .sensorModelId(UUID.randomUUID()).sensorStatusId(UUID.randomUUID()).build()));
        when(installationRepository.existsOverlapForSensor(sensorId, installedAt)).thenReturn(false);
        when(installationRepository.save(any())).thenAnswer(invocation -> {
            SensorInstallation saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        SensorInstallation created = installationService.create(sensorId, environmentId, installedAt);

        verify(outboxWriter).append(
                eq(SensorInstalled.TYPE),
                eq(SensorInstalled.AGGREGATE_TYPE),
                eq(created.getId().toString()),
                eq(SensorInstalled.ROUTING_KEY),
                any(SensorInstalled.class),
                eq(installedAt));
    }

    @Test
    void closeEnqueuesSensorRemoved() {
        UUID id = UUID.randomUUID();
        SensorInstallation installation = SensorInstallation.builder()
                .id(id)
                .sensorId(UUID.randomUUID())
                .educationalEnvironmentId(UUID.randomUUID())
                .installedAt(Instant.now().minus(2, ChronoUnit.DAYS))
                .build();
        when(installationRepository.findById(id)).thenReturn(Optional.of(installation));
        when(installationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SensorInstallation closed = installationService.close(id);

        verify(outboxWriter).append(
                eq(SensorRemoved.TYPE),
                eq(SensorRemoved.AGGREGATE_TYPE),
                eq(id.toString()),
                eq(SensorRemoved.ROUTING_KEY),
                any(SensorRemoved.class),
                eq(closed.getRemovedAt()));
    }

    @Test
    void closeRejectsAlreadyClosedInstallation() {
        UUID id = UUID.randomUUID();
        SensorInstallation closed = SensorInstallation.builder()
                .id(id)
                .sensorId(UUID.randomUUID())
                .educationalEnvironmentId(UUID.randomUUID())
                .installedAt(Instant.now().minus(2, ChronoUnit.DAYS))
                .removedAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build();
        when(installationRepository.findById(id)).thenReturn(Optional.of(closed));

        assertThatThrownBy(() -> installationService.close(id))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void closeRejectsInstallationWithFutureInstalledAt() {
        UUID id = UUID.randomUUID();
        SensorInstallation future = SensorInstallation.builder()
                .id(id)
                .sensorId(UUID.randomUUID())
                .educationalEnvironmentId(UUID.randomUUID())
                .installedAt(Instant.now().plus(2, ChronoUnit.DAYS))
                .build();
        when(installationRepository.findById(id)).thenReturn(Optional.of(future));

        assertThatThrownBy(() -> installationService.close(id))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void closeSetsRemovedAt() {
        UUID id = UUID.randomUUID();
        SensorInstallation installation = SensorInstallation.builder()
                .id(id)
                .sensorId(UUID.randomUUID())
                .educationalEnvironmentId(UUID.randomUUID())
                .installedAt(Instant.now().minus(2, ChronoUnit.DAYS))
                .build();
        when(installationRepository.findById(id)).thenReturn(Optional.of(installation));
        when(installationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SensorInstallation closed = installationService.close(id);

        assertThat(closed.getRemovedAt()).isNotNull();
        assertThat(closed.isActive()).isFalse();
    }
}
