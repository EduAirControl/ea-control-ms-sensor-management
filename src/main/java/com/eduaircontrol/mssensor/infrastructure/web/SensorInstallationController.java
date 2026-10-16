package com.eduaircontrol.mssensor.infrastructure.web;

import com.eduaircontrol.mssensor.application.SensorInstallationService;
import com.eduaircontrol.mssensor.domain.model.PageResult;
import com.eduaircontrol.mssensor.domain.model.SensorInstallation;
import com.eduaircontrol.mssensor.infrastructure.web.dto.PageResponse;
import com.eduaircontrol.mssensor.infrastructure.web.dto.SensorInstallationCreateRequest;
import com.eduaircontrol.mssensor.infrastructure.web.dto.SensorInstallationResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/sensor-installations")
@RequiredArgsConstructor
public class SensorInstallationController {

    private final SensorInstallationService installationService;

    @GetMapping
    public PageResponse<SensorInstallationResponse> list(
            @RequestParam(name = "sensorId", required = false) UUID sensorId,
            @RequestParam(name = "educationalEnvironmentId", required = false) UUID educationalEnvironmentId,
            @RequestParam(name = "active", required = false) Boolean active,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        PageParams.validate(page, limit);
        PageResult<SensorInstallation> result = installationService.list(
                sensorId, educationalEnvironmentId, active, page, limit);
        return PageResponse.of(result, SensorInstallationResponse::from);
    }

    @GetMapping("/{id}")
    public SensorInstallationResponse get(@PathVariable UUID id) {
        return SensorInstallationResponse.from(installationService.get(id));
    }

    @PostMapping
    public ResponseEntity<SensorInstallationResponse> create(
            @Valid @RequestBody SensorInstallationCreateRequest request) {
        SensorInstallation installation = installationService.create(
                request.sensorId(), request.educationalEnvironmentId(), request.installedAt());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(installation.getId())
                .toUri();
        return ResponseEntity.created(location).body(SensorInstallationResponse.from(installation));
    }

    @PostMapping("/{id}/remove")
    public SensorInstallationResponse close(@PathVariable UUID id) {
        return SensorInstallationResponse.from(installationService.close(id));
    }

    /**
     * Borrado <b>fisico</b> de la instalacion (purga), restringido a ADMIN por la
     * regla global de DELETE de {@code SecurityConfig}.
     *
     * <p>Es la operacion que limpia datos de prueba o un alta por error. Borra la
     * fila y publica el evento que hace que ms-environment-monitoring purgue su
     * proyeccion y las mediciones que la referenciaban. Para el caso normal de
     * negocio (el sensor se retira pero el historial importa) sigue estando
     * {@code POST /{id}/remove}, que cierra con {@code removedAt}.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        installationService.delete(id);
    }
}
