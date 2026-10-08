package com.eduaircontrol.mssensor.infrastructure.persistence;

import com.eduaircontrol.mssensor.domain.model.Device;
import com.eduaircontrol.mssensor.domain.model.PageResult;
import com.eduaircontrol.mssensor.domain.port.out.DeviceRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DeviceRepositoryAdapter implements DeviceRepository {

    private final DeviceJpaRepository jpaRepository;

    @Override
    public Device save(Device device) {
        return jpaRepository.save(device);
    }

    @Override
    public Optional<Device> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<Device> findByMacAddress(String macAddress) {
        return jpaRepository.findByMacAddress(macAddress);
    }

    @Override
    public PageResult<Device> search(UUID educationalEnvironmentId, String status, int page, int limit) {
        Specification<Device> specification = (root, q, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (educationalEnvironmentId != null) {
                predicates.add(cb.equal(root.get("educationalEnvironmentId"), educationalEnvironmentId));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return predicates.isEmpty() ? cb.conjunction()
                    : cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        var result = jpaRepository.findAll(specification,
                PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new PageResult<>(result.getContent(), result.getTotalElements(), page, limit);
    }

    @Override
    public boolean existsByMacAddress(String macAddress) {
        return jpaRepository.existsByMacAddress(macAddress);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}