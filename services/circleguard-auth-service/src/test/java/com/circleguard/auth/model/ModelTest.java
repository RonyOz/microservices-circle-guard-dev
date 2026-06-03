package com.circleguard.auth.model;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ModelTest {

    // ── Permission ────────────────────────────────────────────────────────────

    @Test
    void permission_builder_setsAllFields() {
        UUID id = UUID.randomUUID();
        Permission p = Permission.builder()
                .id(id)
                .name("PERM_READ")
                .description("Read access")
                .build();

        assertThat(p.getId()).isEqualTo(id);
        assertThat(p.getName()).isEqualTo("PERM_READ");
        assertThat(p.getDescription()).isEqualTo("Read access");
    }

    @Test
    void permission_setters_updateFields() {
        Permission p = new Permission();
        UUID id = UUID.randomUUID();
        p.setId(id);
        p.setName("PERM_WRITE");
        p.setDescription("Write access");

        assertThat(p.getId()).isEqualTo(id);
        assertThat(p.getName()).isEqualTo("PERM_WRITE");
        assertThat(p.getDescription()).isEqualTo("Write access");
    }

    @Test
    void permission_equals_andHashCode_basedOnFields() {
        UUID id = UUID.randomUUID();
        Permission a = Permission.builder().id(id).name("PERM_READ").build();
        Permission b = Permission.builder().id(id).name("PERM_READ").build();
        Permission c = Permission.builder().id(UUID.randomUUID()).name("PERM_WRITE").build();

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a).isNotEqualTo(c);
    }

    @Test
    void permission_toString_containsName() {
        Permission p = Permission.builder().name("PERM_ADMIN").build();
        assertThat(p.toString()).contains("PERM_ADMIN");
    }

    @Test
    void permission_noArgsConstructor_createsEmptyInstance() {
        Permission p = new Permission();
        assertThat(p.getId()).isNull();
        assertThat(p.getName()).isNull();
    }

    @Test
    void permission_allArgsConstructor_setsAllFields() {
        UUID id = UUID.randomUUID();
        Permission p = new Permission(id, "PERM_DELETE", "Delete access");
        assertThat(p.getId()).isEqualTo(id);
        assertThat(p.getName()).isEqualTo("PERM_DELETE");
        assertThat(p.getDescription()).isEqualTo("Delete access");
    }

    // ── Role ──────────────────────────────────────────────────────────────────

    @Test
    void role_builder_setsAllFields() {
        UUID id = UUID.randomUUID();
        Permission perm = Permission.builder().name("PERM_READ").build();
        Role r = Role.builder()
                .id(id)
                .name("ADMIN")
                .description("Administrator role")
                .permissions(Set.of(perm))
                .build();

        assertThat(r.getId()).isEqualTo(id);
        assertThat(r.getName()).isEqualTo("ADMIN");
        assertThat(r.getDescription()).isEqualTo("Administrator role");
        assertThat(r.getPermissions()).containsExactly(perm);
    }

    @Test
    void role_setters_updateFields() {
        Role r = new Role();
        UUID id = UUID.randomUUID();
        r.setId(id);
        r.setName("STUDENT");
        r.setDescription("Student role");
        r.setPermissions(Set.of());

        assertThat(r.getId()).isEqualTo(id);
        assertThat(r.getName()).isEqualTo("STUDENT");
        assertThat(r.getPermissions()).isEmpty();
    }

    @Test
    void role_equals_andHashCode_basedOnFields() {
        UUID id = UUID.randomUUID();
        Role a = Role.builder().id(id).name("ADMIN").build();
        Role b = Role.builder().id(id).name("ADMIN").build();
        Role c = Role.builder().id(UUID.randomUUID()).name("USER").build();

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a).isNotEqualTo(c);
    }

    @Test
    void role_toString_containsName() {
        Role r = Role.builder().name("HEALTH_OFFICER").build();
        assertThat(r.toString()).contains("HEALTH_OFFICER");
    }

    @Test
    void role_noArgsConstructor_createsEmptyInstance() {
        Role r = new Role();
        assertThat(r.getId()).isNull();
        assertThat(r.getPermissions()).isNull();
    }

    // ── LocalUser ─────────────────────────────────────────────────────────────

    @Test
    void localUser_builder_setsAllFields() {
        UUID id = UUID.randomUUID();
        Role role = Role.builder().name("STUDENT").build();
        LocalUser u = LocalUser.builder()
                .id(id)
                .username("alice")
                .password("hashed-pw")
                .email("alice@circleguard.edu")
                .isActive(true)
                .roles(Set.of(role))
                .build();

        assertThat(u.getId()).isEqualTo(id);
        assertThat(u.getUsername()).isEqualTo("alice");
        assertThat(u.getPassword()).isEqualTo("hashed-pw");
        assertThat(u.getEmail()).isEqualTo("alice@circleguard.edu");
        assertThat(u.getIsActive()).isTrue();
        assertThat(u.getRoles()).containsExactly(role);
    }

    @Test
    void localUser_setters_updateFields() {
        LocalUser u = new LocalUser();
        UUID id = UUID.randomUUID();
        u.setId(id);
        u.setUsername("bob");
        u.setPassword("pw-hash");
        u.setEmail("bob@uni.edu");
        u.setIsActive(false);
        u.setRoles(Set.of());

        assertThat(u.getId()).isEqualTo(id);
        assertThat(u.getUsername()).isEqualTo("bob");
        assertThat(u.getIsActive()).isFalse();
        assertThat(u.getRoles()).isEmpty();
    }

    @Test
    void localUser_equals_andHashCode_basedOnFields() {
        UUID id = UUID.randomUUID();
        LocalUser a = LocalUser.builder().id(id).username("alice").password("pw").build();
        LocalUser b = LocalUser.builder().id(id).username("alice").password("pw").build();
        LocalUser c = LocalUser.builder().id(UUID.randomUUID()).username("charlie").password("pw").build();

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a).isNotEqualTo(c);
    }

    @Test
    void localUser_toString_containsUsername() {
        LocalUser u = LocalUser.builder().username("diana").build();
        assertThat(u.toString()).contains("diana");
    }

    @Test
    void localUser_noArgsConstructor_createsEmptyInstance() {
        LocalUser u = new LocalUser();
        assertThat(u.getId()).isNull();
        assertThat(u.getUsername()).isNull();
    }

    @Test
    void localUser_allArgsConstructor_setsAllFields() {
        UUID id = UUID.randomUUID();
        LocalUser u = new LocalUser(id, "eve", "hash", "eve@uni.edu", true, Set.of());
        assertThat(u.getId()).isEqualTo(id);
        assertThat(u.getUsername()).isEqualTo("eve");
        assertThat(u.getIsActive()).isTrue();
    }
}
