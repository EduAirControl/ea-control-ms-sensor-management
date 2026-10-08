package com.eduaircontrol.mssensor.infrastructure.web;

import com.eduaircontrol.mssensor.application.DeviceService;
import com.eduaircontrol.mssensor.domain.model.Device;
import com.eduaircontrol.mssensor.infrastructure.web.dto.DeviceDtos.DeviceCreateRequest;
import com.eduaircontrol.mssensor.infrastructure.web.dto.DeviceDtos.DeviceResponse;
import com.eduaircontrol.mssensor.infrastructure.web.dto.DeviceDtos.DeviceUpdateRequest;
import com.eduaircontrol.mssensor.infrastructure.web.dto.PageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registro de nodos ESP32 (HU de dispositivos IoT).
 *
 * <p>Es lo que le faltaba a los microservicios: la app movil llamaba a
 * {@code /api/v1/devices} contra el monolito y en este despliegue no existia.
 *
 * <p>La escritura es ADMIN porque registrar un nodo es dar de alta hardware en la
 * plataforma. La lectura admite USER para que un docente vea que aula tiene nodo.
 */
@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping
    public PageResponse<DeviceResponse> list(
            @RequestParam(name = "educationalEnvironmentId", required = false) UUID educationalEnvironmentId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        PageParams.validate(page, limit);
        return PageResponse.of(
                deviceService.list(educationalEnvironmentId, status, page, limit),
                DeviceResponse::from);
    }

    @GetMapping("/{id}")
    public DeviceResponse get(@PathVariable UUID id) {
        return DeviceResponse.from(deviceService.get(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<DeviceResponse> create(@Valid @RequestBody DeviceCreateRequest request) {
        Device created = deviceService.create(request);
        return ResponseEntity.status(201).body(DeviceResponse.from(created));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    public DeviceResponse update(@PathVariable UUID id,
            @Valid @RequestBody DeviceUpdateRequest request) {
        return DeviceResponse.from(deviceService.update(id, request));
    }

    /** Marca el nodo como conectado con la hora actual. */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/touch")
    public DeviceResponse touch(@PathVariable UUID id,
            @RequestParam(name = "ssid", required = false) String ssid) {
        return DeviceResponse.from(deviceService.touch(id, ssid));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deviceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}