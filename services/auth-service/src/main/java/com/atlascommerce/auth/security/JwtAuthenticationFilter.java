package com.atlascommerce.auth.security;

import java.io.IOException;
import java.util.Date;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.atlascommerce.auth.service.TokenBlacklistService;
import com.atlascommerce.auth.service.UserTokenInvalidationService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
     private final JwtService jwtService;
     private final CustomUserDetailsService userDetailsService;
     private final TokenBlacklistService tokenBlacklistService;
     private final UserTokenInvalidationService userTokenInvalidationService;

     public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService, 
        TokenBlacklistService tokenBlacklistService, UserTokenInvalidationService userTokenInvalidationService) {
         this.jwtService = jwtService;
         this.userDetailsService = userDetailsService;
         this.tokenBlacklistService = tokenBlacklistService;
         this.userTokenInvalidationService = userTokenInvalidationService;
     }

     @Override
     protected void doFilterInternal(
        HttpServletRequest request, 
        HttpServletResponse response, 
        FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getServletPath();

        if (isPublicEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7).trim();

        if (tokenBlacklistService.isBlacklisted(jwt)) {
            log.warn("Blacklisted token used on {}", request.getRequestURI());
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try {
            String username = jwtService.extractUsername(jwt);
            String tokenType = jwtService.extractTokenType(jwt);
            Date issuedAt = jwtService.extractIssuedAt(jwt);

            if (!"ACCESS".equals(tokenType)) {
                log.warn("Received non-ACCESS token type '{}' on protected endpoint: {}", 
                        tokenType, request.getRequestURI());

                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            if (userTokenInvalidationService.isTokenRevokedForUser(username, issuedAt)) {
                log.warn("Token issued before user-wide revocation. User: {}", username);
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                
                if (jwtService.isValidToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = 
                        new UsernamePasswordAuthenticationToken(
                            userDetails, 
                            null, 
                            userDetails.getAuthorities()
                        );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));                    
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    log.debug("User {} authenticated successfully via JWT", username);
                } else {
                    log.warn("Token validation failed for user: {}", username);
                    SecurityContextHolder.clearContext();
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
            }
            
        } catch (Exception ex) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            log.error("JWT processing failed for request {}: {}", 
                      request.getRequestURI(), ex.getMessage());
            
            SecurityContextHolder.clearContext();
            return;
        }        
         
        filterChain.doFilter(request, response);
     }    

     private boolean isPublicEndpoint(String path) {
        return path.equals("/api/v1/auth/register")
            || path.equals("/api/v1/auth/login")
            || path.equals("/api/v1/auth/refresh-token")
            || path.equals("/api/v1/auth/logout")
            || path.startsWith("/actuator/")
            || path.equals("/actuator")
            || path.startsWith("/h2-console");
    }
}