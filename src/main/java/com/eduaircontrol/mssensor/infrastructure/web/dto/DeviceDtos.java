package com.eduaircontrol.mssensor.infrastructure.web.dto;

import com.eduaircontrol.mssensor.domain.model.Device;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public class DeviceDtos {

    private DeviceDtos() {
    }

    /** Formato que el firmware ESP32 imprime por el Monitor Serie. */
    public static final String MAC_PATTERN = "^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$";

    public record DeviceCreateRequest(
            @NotBlank(message = "macAddress es obligatoria")
            @Pattern(regexp = MAC_PATTERN,
                    message = "macAddress debe tener formato AA:BB:CC:DD:EE:FF")
            String macAddress,

            @Size(max = 120) String name,

            @Size(max = 20) String deviceType,

            @Size(max = 40) String firmwareVersion,

            @Size(max = 64) String ssid,

            UUID educationalEnvironmentId) {
    }

    /** Actualizacion parcial: un null conserva el valor actual. */
    public record DeviceUpdateRequest(
            @Size(max = 120) String name,
            @Size(max = 20) String deviceType,
            @Size(max = 40) String firmwareVersion,
            @Size(max = 64) String ssid,
            String status,
            UUID educationalEnvironmentId) {
    }

    public record DeviceResponse(
            UUID deviceId,
            String macAddress,
            String name,
            String deviceType,
            String firmwareVersion,
            String ssid,
            String status,
            UUID educationalEnvironmentId,
            Instant lastSeenAt,
            Instant createdAt,
            Instant updatedAt) {

        public static DeviceResponse from(Device device) {
            return new DeviceResponse(
                    device.getId(),
                    device.getMacAddress(),
                    device.getName(),
                    device.getDeviceType(),
                    device.getFirmwareVersion(),
                    device.getSsid(),
                    device.getStatus(),
                    device.getEducationalEnvironmentId(),
                    device.getLastSeenAt(),
                    device.getCreatedAt(),
                    device.getUpdatedAt());
        }
    }
}