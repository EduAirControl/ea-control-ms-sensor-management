package com.eduaircontrol.mssensor.application;

import com.eduaircontrol.mssensor.application.page.PageResult;
import com.eduaircontrol.mssensor.application.port.SensorVariableRepository;
import com.eduaircontrol.mssensor.application.port.VariableRepository;
import com.eduaircontrol.mssensor.domain.exception.ConflictException;
import com.eduaircontrol.mssensor.domain.exception.NotFoundException;
import com.eduaircontrol.mssensor.domain.model.Variable;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class VariableService {

    private final VariableRepository variableRepository;
    private final SensorVariableRepository sensorVariableRepository;

    @Transactional(readOnly = true)
    public PageResult<Variable> list(String query, int page, int limit) {
        return variableRepository.search(query, page, limit);
    }

    @Transactional(readOnly = true)
    public Variable get(UUID id) {
        return variableRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Variable not found: " + id));
    }

    public Variable create(String code, String name, UUID measurementUnitId, String description) {
        String normalizedCode = SensorService.requireText(code, "code").toUpperCase();
        if (variableRepository.existsByCode(normalizedCode)) {
            throw new ConflictException("Variable code already exists: " + normalizedCode);
        }
        Variable variable = Variable.builder()
                .code(normalizedCode)
                .name(SensorService.requireText(name, "name"))
                .measurementUnitId(SensorService.requireId(measurementUnitId, "measurementUnitId"))
                .description(SensorService.emptyToNull(description))
                .build();
        return variableRepository.save(variable);
    }

    public void delete(UUID id) {
        Variable variable = get(id);
        if (sensorVariableRepository.existsByVariableId(id)) {
            throw new ConflictException("Variable is referenced by sensors and cannot be deleted: " + id);
        }
        variableRepository.delete(variable);
    }
}
