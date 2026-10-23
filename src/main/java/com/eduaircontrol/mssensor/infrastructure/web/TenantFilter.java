package com.eduaircontrol.mssensor.infrastructure.web;

import com.eduaircontrol.mssensor.application.TenantContext;
import com.eduaircontrol.mssensor.shared.security.JwtAuthFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Publica el tenant del JWT en {@code TenantContext}. El valor sale de los
 * atributos que {@link JwtAuthFilter} publica desde los claims
 * institutionId/campusId: los headers del gateway no se aceptan como tenant
 * (R-001). El orden importa: JwtAuthFilter corre dentro de la cadena de
 * seguridad (orden -100), asi que este filtro va detras de ella.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            TenantContext.set(
                    (UUID) request.getAttribute(JwtAuthFilter.ATTR_INSTITUTION_ID),
                    (UUID) request.getAttribute(JwtAuthFilter.ATTR_CAMPUS_ID));
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
