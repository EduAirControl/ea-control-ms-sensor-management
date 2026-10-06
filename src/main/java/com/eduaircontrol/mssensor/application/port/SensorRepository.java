package com.eduaircontrol.mssensor.application.port;

import com.eduaircontrol.mssensor.application.page.PageResult;
import com.eduaircontrol.mssensor.domain.model.Sensor;
import java.util.Optional;
import java.util.UUID;

public interface SensorRepository {

    Sensor save(Sensor sensor);

    Optional<Sensor> findById(UUID id);

    boolean existsBySerialNumber(String serialNumber);

    void delete(Sensor sensor);

    PageResult<Sensor> search(String query, UUID sensorModelId, UUID sensorStatusId, int page, int limit);
}
