package com.eduaircontrol.mssensor.application.port;

import com.eduaircontrol.mssensor.application.page.PageResult;
import com.eduaircontrol.mssensor.domain.model.Variable;
import java.util.Optional;
import java.util.UUID;

public interface VariableRepository {

    Variable save(Variable variable);

    Optional<Variable> findById(UUID id);

    boolean existsByCode(String code);

    void delete(Variable variable);

    PageResult<Variable> search(String query, int page, int limit);
}
