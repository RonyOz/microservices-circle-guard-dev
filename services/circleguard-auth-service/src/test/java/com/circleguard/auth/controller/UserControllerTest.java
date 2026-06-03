package com.circleguard.auth.controller;

import com.circleguard.auth.model.LocalUser;
import com.circleguard.auth.repository.LocalUserRepository;
import com.circleguard.auth.security.SecurityConfig;
import com.circleguard.auth.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LocalUserRepository localUserRepository;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private AuthenticationManager authenticationManager;

    @Test
    @WithMockUser
    void getUsersByPermission_returnsMatchingUsers() throws Exception {
        LocalUser alice = LocalUser.builder().username("alice").email("alice@circleguard.edu").build();
        LocalUser bob = LocalUser.builder().username("bob").email("bob@circleguard.edu").build();
        when(localUserRepository.findUsersByPermissionName("HEALTH_OFFICER"))
                .thenReturn(List.of(alice, bob));

        mockMvc.perform(get("/api/v1/users/permissions/HEALTH_OFFICER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].username").value("alice"))
                .andExpect(jsonPath("$[0].email").value("alice@circleguard.edu"))
                .andExpect(jsonPath("$[1].username").value("bob"));
    }

    @Test
    @WithMockUser
    void getUsersByPermission_nullEmail_isMappedToEmptyString() throws Exception {
        LocalUser user = LocalUser.builder().username("charlie").email(null).build();
        when(localUserRepository.findUsersByPermissionName("PERM_READ"))
                .thenReturn(List.of(user));

        mockMvc.perform(get("/api/v1/users/permissions/PERM_READ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("charlie"))
                .andExpect(jsonPath("$[0].email").value(""));
    }

    @Test
    @WithMockUser
    void getUsersByPermission_emptyList_returnsEmptyArray() throws Exception {
        when(localUserRepository.findUsersByPermissionName("NONEXISTENT")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/users/permissions/NONEXISTENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
