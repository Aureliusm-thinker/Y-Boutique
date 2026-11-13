package com.yvette.api.staff;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * When the store requires two-step sign-in ({@code yvette.security.mfa-required}, on by default),
 * a staff session opened without an authenticator code can only reach "who am I" and the
 * two-step setup endpoints. Everything else answers 403 with {@code code: mfa_setup_required}.
 */
@Configuration
public class MfaEnforcement implements WebMvcConfigurer, HandlerInterceptor {

    private final boolean required;

    public MfaEnforcement(@Value("${yvette.security.mfa-required:true}") boolean required) {
        this.required = required;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this)
                .addPathPatterns("/api/v1/admin/**")
                .excludePathPatterns("/api/v1/admin/me", "/api/v1/admin/me/mfa/**");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!required) return true;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt
                && Boolean.TRUE.equals(jwt.getClaimAsBoolean(TokenService.MFA_CLAIM))) {
            return true;
        }
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write("""
                {"status":403,"title":"Forbidden","code":"mfa_setup_required",\
                "detail":"Set up two-step sign-in to continue."}""");
        return false;
    }
}
