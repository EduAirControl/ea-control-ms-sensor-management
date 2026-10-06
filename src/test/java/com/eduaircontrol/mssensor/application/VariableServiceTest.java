package com.eduaircontrol.mssensor.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eduaircontrol.mssensor.application.port.SensorVariableRepository;
import com.eduaircontrol.mssensor.application.port.VariableRepository;
import com.eduaircontrol.mssensor.domain.exception.ConflictException;
import com.eduaircontrol.mssensor.domain.exception.NotFoundException;
import com.eduaircontrol.mssensor.domain.model.Variable;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VariableServiceTest {

    private VariableRepository variableRepository;
    private SensorVariableRepository sensorVariableRepository;
    private VariableService variableService;

    @BeforeEach
    void setUp() {
        variableRepository = mock(VariableRepository.class);
        sensorVariableRepository = mock(SensorVariableRepository.class);
        variableService = new VariableService(variableRepository, sensorVariableRepository);
    }

    @Test
    void createUppercasesCodeAndRejectsDuplicates() {
        when(variableRepository.existsByCode("TEMP")).thenReturn(true);

        assertThatThrownBy(() -> variableService.create("temp", "Temperature", UUID.randomUUID(), null))
                .isInstanceOf(ConflictException.class);

        verify(variableRepository, never()).save(any());
    }

    @Test
    void createTrimsAndDefaultsDescriptionToNull() {
        when(variableRepository.existsByCode("CO2")).thenReturn(false);
        when(variableRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Variable created = variableService.create("co2", "Carbon dioxide", UUID.randomUUID(), "  ");

        assertThat(created.getCode()).isEqualTo("CO2");
        assertThat(created.getDescription()).isNull();
    }

    @Test
    void deleteRejectsVariableReferencedBySensors() {
        UUID id = UUID.randomUUID();
        when(variableRepository.findById(id)).thenReturn(Optional.of(
                Variable.builder().id(id).code("TEMP").name("Temperature")
                        .measurementUnitId(UUID.randomUUID()).build()));
        when(sensorVariableRepository.existsByVariableId(id)).thenReturn(true);

        assertThatThrownBy(() -> variableService.delete(id))
                .isInstanceOf(ConflictException.class);

        verify(variableRepository, never()).delete(any());
    }

    @Test
    void deleteRejectsUnknownVariable() {
        UUID id = UUID.randomUUID();
        when(variableRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> variableService.delete(id))
                .isInstanceOf(NotFoundException.class);
    }
}
