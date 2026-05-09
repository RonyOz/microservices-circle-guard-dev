package com.circleguard.auth.service;

import com.circleguard.auth.model.LocalUser;
import com.circleguard.auth.model.Role;
import com.circleguard.auth.model.Permission;
import com.circleguard.auth.repository.LocalUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class CustomUserDetailsServiceTest {

    private LocalUserRepository repoWithUser(LocalUser user) {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("findByUsername".equals(method.getName()) && args != null && args.length == 1) {
                return Optional.ofNullable(user);
            }
            throw new UnsupportedOperationException("Not implemented in test stub: " + method.getName());
        };
        return (LocalUserRepository) Proxy.newProxyInstance(
                LocalUserRepository.class.getClassLoader(),
                new Class[]{LocalUserRepository.class},
                handler
        );
    }

    private LocalUserRepository repoEmpty() {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("findByUsername".equals(method.getName()) && args != null && args.length == 1) {
                return Optional.empty();
            }
            throw new UnsupportedOperationException("Not implemented in test stub: " + method.getName());
        };
        return (LocalUserRepository) Proxy.newProxyInstance(
                LocalUserRepository.class.getClassLoader(),
                new Class[]{LocalUserRepository.class},
                handler
        );
    }

    @Test
    void loadUserByUsername_userFound_returnsUserDetailsWithAuthorities() {
        Role role = Role.builder().name("ADMIN").build();
        Permission perm = Permission.builder().name("user:read").build();
        role.setPermissions(Set.of(perm));

        LocalUser user = LocalUser.builder()
                .username("alice")
                .password("hashedpass")
                .isActive(true)
                .roles(Set.of(role))
                .build();

        LocalUserRepository repo = repoWithUser(user);

        CustomUserDetailsService svc = new CustomUserDetailsService(repo);
        UserDetails ud = svc.loadUserByUsername("alice");

        assertEquals("alice", ud.getUsername());
        assertEquals("hashedpass", ud.getPassword());
        assertTrue(ud.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(ud.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("user:read")));
    }

    @Test
    void loadUserByUsername_disabled_throwsDisabledException() {
        LocalUser user = LocalUser.builder().username("bob").password("p").isActive(false).build();
        LocalUserRepository repo = repoWithUser(user);

        CustomUserDetailsService svc = new CustomUserDetailsService(repo);
        assertThrows(DisabledException.class, () -> svc.loadUserByUsername("bob"));
    }

    @Test
    void loadUserByUsername_notFound_throwsUsernameNotFoundException() {
        LocalUserRepository repo = repoEmpty();

        CustomUserDetailsService svc = new CustomUserDetailsService(repo);
        assertThrows(UsernameNotFoundException.class, () -> svc.loadUserByUsername("charlie"));
    }
}
