package com.circleguard.auth.controller;

import com.circleguard.auth.model.LocalUser;
import com.circleguard.auth.repository.LocalUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final LocalUserRepository localUserRepository;

    @GetMapping("/permissions/{permissionName}")
    public ResponseEntity<List<Map<String, String>>> getUsersByPermission(@PathVariable String permissionName) {
        List<LocalUser> users = localUserRepository.findUsersByPermissionName(permissionName);

        List<Map<String, String>> response = users.stream()
                .map(u -> Map.of(
                        "username", u.getUsername(),
                        "email", u.getEmail() != null ? u.getEmail() : ""
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
