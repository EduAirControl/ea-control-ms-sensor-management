package com.eduaircontrol.mssensor.infrastructure.persistence;

import com.eduaircontrol.mssensor.domain.model.Device;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

// JpaSpecificationExecutor es lo que aporta findAll(Specification, Pageable); un
// JpaRepository a secas no expone esa sobrecarga.
public interface DeviceJpaRepository
        extends JpaRepository<Device, UUID>, JpaSpecificationExecutor<Device> {

    Optional<Device> findByMacAddress(String macAddress);

    boolean existsByMacAddress(String macAddress);
}