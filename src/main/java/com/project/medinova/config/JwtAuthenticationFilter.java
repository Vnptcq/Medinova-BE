package com.project.medinova.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    @Lazy
    private com.project.medinova.service.AuthService authService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        
        // Skip filter for root path, Swagger UI and authentication endpoints only
        // Tất cả các endpoint khác yêu cầu authentication (không có public endpoints)
        if (path.equals("/") || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs") || 
            path.startsWith("/swagger-resources") || path.startsWith("/webjars") ||
            path.startsWith("/api/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt) && !authService.isTokenBlacklisted(jwt)) {
                String username = tokenProvider.getUsernameFromToken(jwt);
                Long userId = tokenProvider.getUserIdFromToken(jwt);
                String role = tokenProvider.getRoleFromToken(jwt);

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                
                // Tạo custom authentication với userId và role trong details
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                
                // Lưu userId và role vào authentication details để có thể lấy sau
                java.util.Map<String, Object> details = new java.util.HashMap<>();
                if (userId != null) {
                    details.put("userId", userId);
                }
                if (role != null) {
                    details.put("role", role);
                }
                
                // Giữ lại thông tin WebAuthenticationDetails cơ bản
                org.springframework.security.web.authentication.WebAuthenticationDetails webDetails = 
                        new WebAuthenticationDetailsSource().buildDetails(request);
                details.put("remoteAddress", webDetails.getRemoteAddress());
                details.put("sessionId", webDetails.getSessionId());
                
                authentication.setDetails(details);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}

