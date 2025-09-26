package com.bob.mta.common.security;

<<<<<<< HEAD
import io.jsonwebtoken.Claims;
=======
>>>>>>> origin/main
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
<<<<<<< HEAD
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
=======
import java.io.IOException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads bearer tokens from the Authorization header and populates the security context.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(final JwtTokenProvider tokenProvider) {
>>>>>>> origin/main
        this.tokenProvider = tokenProvider;
    }

    @Override
<<<<<<< HEAD
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null) {
            Claims claims = tokenProvider.parseClaims(token);
            String userId = claims.getSubject();
            String username = claims.get("username", String.class);
            List<String> roles = claims.get("roles", List.class);
            Collection<? extends GrantedAuthority> authorities = roles == null ? List.of()
                    : roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    new JwtUserDetails(userId, username, authorities),
                    token,
                    authorities
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
=======
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain)
            throws ServletException, IOException {
        final String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            final String token = header.substring(BEARER_PREFIX.length());
            final Optional<JwtTokenProvider.TokenPayload> payload = tokenProvider.parseToken(token);
            payload.ifPresent(value -> setAuthentication(request, value));
>>>>>>> origin/main
        }
        filterChain.doFilter(request, response);
    }

<<<<<<< HEAD
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
=======
    private void setAuthentication(final HttpServletRequest request, final JwtTokenProvider.TokenPayload payload) {
        final UserDetails principal = User.withUsername(payload.username())
                .password("N/A")
                .roles(payload.role())
                .build();
        final UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("Authenticated request for user {}", payload.username());
>>>>>>> origin/main
    }
}
