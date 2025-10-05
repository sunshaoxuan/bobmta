package com.bob.mta.modules.user.service.model;

import com.bob.mta.modules.user.domain.User;
import com.bob.mta.modules.user.domain.UserStatus;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Spring Security {@link UserDetails} projection backed by the {@link User} aggregate.
 */
public class UserPrincipal implements UserDetails {

    private final String id;
    private final String username;
    private final String passwordHash;
    private final String displayName;
    private final UserStatus status;
    private final List<String> roles;
    private final List<GrantedAuthority> authorities;

    private UserPrincipal(
            final String id,
            final String username,
            final String passwordHash,
            final String displayName,
            final UserStatus status,
            final List<String> roles,
            final List<GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.status = status;
        this.roles = roles;
        this.authorities = authorities;
    }

    public static UserPrincipal from(final User user) {
        Objects.requireNonNull(user, "user");
        final List<String> roles = List.copyOf(user.getRoles());
        final List<GrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toUnmodifiableList());
        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getDisplayName(),
                user.getStatus(),
                roles,
                authorities);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getRoles() {
        return roles;
    }

    public UserStatus getStatus() {
        return status;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.SUSPENDED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }
}
