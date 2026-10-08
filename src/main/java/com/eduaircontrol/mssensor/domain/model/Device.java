package com.eduaircontrol.mssensor.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Nodo IoT fisico (ESP32). Se registra en el dominio sensors porque es quien
 * instala y reporta los sensores del ambiente.
 *
 * <p>No guarda ninguna credencial. El {@code clientSecret} vive hasheado en
 * ms-security y el token de aprovisionamiento nunca se persiste: ambos viajan
 * solo en la respuesta. Esta tabla es la identidad visible del nodo.
 */
@Entity
@Table(name = "device", schema = "sensors")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Device {

    /** Estados que la UI de admin conoce; alineados con los chips de la app. */
    public static final String STATUS_PENDING = "pendiente";
    public static final String STATUS_CONNECTED = "conectado";
    public static final String STATUS_OFFLINE = "offline";
    public static final String STATUS_ERROR = "error";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "device_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "mac_address", nullable = false, unique = true)
    private String macAddress;

    @Column(name = "name")
    private String name;

    @Column(name = "device_type", nullable = false)
    private String deviceType;

    @Column(name = "firmware_version")
    private String firmwareVersion;

    @Column(name = "ssid")
    private String ssid;

    @Column(name = "status", nullable = false)
    private String status;

    /** Referencia por UUID a ms-classroom-management (ADR-003, sin FK cruzada). */
    @Column(name = "educational_environment_id")
    private UUID educationalEnvironmentId;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public static String normalizeMac(String macAddress) {
        return macAddress == null ? null : macAddress.trim().toUpperCase();
    }
}