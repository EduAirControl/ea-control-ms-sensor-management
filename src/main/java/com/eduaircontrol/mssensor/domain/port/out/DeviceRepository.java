package com.eduaircontrol.mssensor.domain.port.out;

import com.eduaircontrol.mssensor.domain.model.Device;
import com.eduaircontrol.mssensor.domain.model.PageResult;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida de dispositivos. Los puertos no exponen tipos de Spring Data,
 * de ahi el {@link PageResult} propio en lugar de {@code org.springframework.data.domain.Page}.
 */
public interface DeviceRepository {

    Device save(Device device);

    Optional<Device> findById(UUID id);

    Optional<Device> findByMacAddress(String macAddress);

    PageResult<Device> search(UUID educationalEnvironmentId, String status, int page, int limit);

    boolean existsByMacAddress(String macAddress);

    void deleteById(UUID id);
}