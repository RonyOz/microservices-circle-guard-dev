package com.circleguard.auth.repository;

import com.circleguard.auth.model.LocalUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface LocalUserRepository extends JpaRepository<LocalUser, UUID> {
    Optional<LocalUser> findByUsername(String username);

    @Query("SELECT u FROM LocalUser u JOIN u.roles r JOIN r.permissions p WHERE p.name = :permissionName")
    List<LocalUser> findUsersByPermissionName(@Param("permissionName") String permissionName);
}
