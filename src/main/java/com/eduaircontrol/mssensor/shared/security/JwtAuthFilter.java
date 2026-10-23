package com.eduaircontrol.mssensor.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autenticacion del servicio: unicamente JWT RS256 validado contra el JWKS de
 * ms-security. Los headers internos del gateway (X-User-*) no se aceptan
 * como identidad (R-001): el gateway reenvia siempre el Authorization
 * resuelto (incluso en el flujo BFF por cookie), asi que las peticiones
 * legitimas llegan con token y quien alcanze el puerto del servicio sin el
 * no obtiene ninguna autoridad (SecurityConfig responde 401).
 *
 * <p>El tenant (institutionId/campusId) sale de los claims del token y se
 * publica como atributos de peticion para que {@code TenantFilter} lo
 * propague a {@code TenantContext}.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    /** Atributo de peticion con el institutionId resuelto del JWT. */
    public static final String ATTR_INSTITUTION_ID = "jwtInstitutionId";

    /** Atributo de peticion con el campusId resuelto del JWT. */
    public static final String ATTR_CAMPUS_ID = "jwtCampusId";

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = authHeader.substring(7);
        try {
            JwtService.AuthenticatedUser user = jwtService.authenticate(token);
            if (user.institutionId() != null) {
                request.setAttribute(ATTR_INSTITUTION_ID, user.institutionId());
            }
            if (user.campusId() != null) {
                request.setAttribute(ATTR_CAMPUS_ID, user.campusId());
            }
            List<SimpleGrantedAuthority> authorities = user.roles().stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                    .toList();
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            user.email() != null ? user.email() : String.valueOf(user.userId()),
                            token,
                            authorities));
        } catch (Exception e) {
            log.debug("Invalid token: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
