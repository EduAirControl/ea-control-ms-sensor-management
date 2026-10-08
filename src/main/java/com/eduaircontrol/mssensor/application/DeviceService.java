package com.eduaircontrol.mssensor.application;

import com.eduaircontrol.mssensor.domain.model.Device;
import com.eduaircontrol.mssensor.domain.model.PageResult;
import com.eduaircontrol.mssensor.domain.port.out.DeviceRepository;
import com.eduaircontrol.mssensor.infrastructure.web.dto.DeviceDtos.DeviceCreateRequest;
import com.eduaircontrol.mssensor.infrastructure.web.dto.DeviceDtos.DeviceUpdateRequest;
import com.eduaircontrol.mssensor.shared.exception.ConflictException;
import com.eduaircontrol.mssensor.shared.exception.NotFoundException;
import com.eduaircontrol.mssensor.shared.exception.ValidationException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro y consulta de nodos ESP32.
 *
 * <p>Deliberadamente no emite credenciales: eso vive en ms-security, unico dueno
 * de {@code oauth2_registered_client}. Aqui solo se guarda la identidad del nodo
 * y el ambiente al que pertenece.
 */
@Service
@RequiredArgsConstructor
public class DeviceService {

    private static final Set<String> STATUSES = Set.of(
            Device.STATUS_PENDING, Device.STATUS_CONNECTED,
            Device.STATUS_OFFLINE, Device.STATUS_ERROR);

    private static final List<String> DEVICE_TYPES = List.of("esp32", "esp32s3", "esp8266", "otro");

    private final DeviceRepository repository;

    @Transactional(readOnly = true)
    public PageResult<Device> list(UUID educationalEnvironmentId, String status, int page, int limit) {
        return repository.search(educationalEnvironmentId, status, page, limit);
    }

    @Transactional(readOnly = true)
    public Device get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Dispositivo no encontrado: " + id));
    }

    @Transactional
    public Device create(DeviceCreateRequest request) {
        String mac = Device.normalizeMac(request.macAddress());
        if (repository.existsByMacAddress(mac)) {
            throw new ConflictException("Ya existe un dispositivo con la MAC " + mac);
        }
        String type = defaultOr(request.deviceType(), "esp32");
        if (!DEVICE_TYPES.contains(type)) {
            throw new ValidationException("deviceType invalido: use uno de " + DEVICE_TYPES);
        }
        return repository.save(Device.builder()
                .macAddress(mac)
                .name(request.name())
                .deviceType(type)
                .firmwareVersion(request.firmwareVersion())
                .ssid(request.ssid())
                .status(Device.STATUS_PENDING)
                .educationalEnvironmentId(request.educationalEnvironmentId())
                .build());
    }

    /** Actualizacion parcial: un null conserva el valor actual. */
    @Transactional
    public Device update(UUID id, DeviceUpdateRequest request) {
        Device device = get(id);
        if (request.name() != null) {
            device.setName(request.name());
        }
        if (request.deviceType() != null) {
            String type = request.deviceType();
            if (!DEVICE_TYPES.contains(type)) {
                throw new ValidationException("deviceType invalido: use uno de " + DEVICE_TYPES);
            }
            device.setDeviceType(type);
        }
        if (request.firmwareVersion() != null) {
            device.setFirmwareVersion(request.firmwareVersion());
        }
        if (request.ssid() != null) {
            device.setSsid(request.ssid());
        }
        if (request.educationalEnvironmentId() != null) {
            device.setEducationalEnvironmentId(request.educationalEnvironmentId());
        }
        if (request.status() != null) {
            if (!STATUSES.contains(request.status())) {
                throw new ValidationException("status invalido: use uno de " + STATUSES);
            }
            device.setStatus(request.status());
        }
        return repository.save(device);
    }

    /** Marca el nodo como conectado y anota la ultima vez que reporto. */
    @Transactional
    public Device touch(UUID id, String ssid) {
        Device device = get(id);
        device.setLastSeenAt(Instant.now());
        device.setStatus(Device.STATUS_CONNECTED);
        if (ssid != null && !ssid.isBlank()) {
            device.setSsid(ssid);
        }
        return repository.save(device);
    }

    @Transactional
    public void delete(UUID id) {
        get(id);
        repository.deleteById(id);
    }

    private static String defaultOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}