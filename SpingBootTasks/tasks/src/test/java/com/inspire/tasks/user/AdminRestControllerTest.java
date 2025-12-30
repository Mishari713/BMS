package com.inspire.tasks.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspire.tasks.auth.TestSecurityConfig;
import com.inspire.tasks.auth.jwt.AuthTokenFilter;
import com.inspire.tasks.auth.dto.SignupRequest;
import com.inspire.tasks.common.MessageResponse;
import com.inspire.tasks.roles.RoleRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminRestController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = AuthTokenFilter.class
        )
)
@Import(TestSecurityConfig.class)
class AdminRestControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Spy
    ObjectMapper objectMapper;

    @MockitoBean
    UserService userService;

    @MockitoBean
    RoleRepository roleRepository;

    @MockitoBean
    PasswordEncoder encoder;

    @WithMockUser(roles = "ADMIN")
    @Test
    void getAllUsers_ReturnsList() throws Exception {
        User u1 = new User();
        u1.setId(1L);
        u1.setUsername("john");

        User u2 = new User();
        u2.setId(2L);
        u2.setUsername("mary");

        when(userService.findAll()).thenReturn(List.of(u1, u2));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].username").value("john"))
                .andExpect(jsonPath("$[1].username").value("mary"));
    }

    @WithMockUser(roles = "ADMIN")
    @Test
    void createUser_ReturnsSuccess() throws Exception {
        SignupRequest request = new SignupRequest();
        request.setUsername("john");
        request.setEmail("john@mail.com");
        request.setPassword("pass123");

        MessageResponse msg = new MessageResponse(200, "User registered successfully!");

        when(userService.createUser(any(SignupRequest.class)))
                .thenAnswer(invocation ->
                                ResponseEntity.ok(new MessageResponse(200, "User registered successfully!")));

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(msg.getMessage()));
    }

    @WithMockUser(roles = "ADMIN")
    @Test
    void updateUser_Success() throws Exception {
        Long userId = 1L;

        User existing = new User("john", "old@mail.com", "pass");
        existing.setId(userId);

        when(userService.findById(userId)).thenReturn(existing);

        when(userService.save(any(User.class)))
                .thenAnswer(invocation ->
                    ResponseEntity.ok(new MessageResponse(200, "User updated successfully!")
                ));


        Map<String, Object> patch = Map.of("email", "new@mail.com");

        mockMvc.perform(patch("/api/admin/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User updated successfully!"));
    }

    @WithMockUser(roles = "ADMIN")
    @Test
    void updateUser_Fails_WhenIdInPayload() throws Exception {
        when(userService.findById(1L)).thenReturn(new User());

        Map<String, Object> patch = Map.of("id", 99);

        mockMvc.perform(patch("/api/admin/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patch)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("User id is not allowed")));
    }

    @WithMockUser(roles = "ADMIN")
    @Test
    void deleteUser_ReturnsSuccess() throws Exception {
        User u = new User();
        u.setId(1L);

        when(userService.findById(1L)).thenReturn(u);
        when(userService.deleteById(1L))
                .thenAnswer(invocation -> ResponseEntity.ok(new MessageResponse(200, "User has been deleted successfully!")));

        mockMvc.perform(delete("/api/admin/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User has been deleted successfully!"));
    }

    @WithMockUser(roles = "ADMIN")
    @Test
    void accessDeniedException_Returns403() throws Exception {
        // Arrange
        when(userService.findAll()).thenThrow(new org.springframework.security.access.AccessDeniedException("Access denied"));

        // Act & Assert
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("You don't have the required role to access this URL"));
    }

    @WithMockUser(roles = "ADMIN")
    @Test
    void genericException_Returns500() throws Exception {
        // Arrange
        when(userService.findAll()).thenThrow(new RuntimeException("Unexpected error"));

        // Assert
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("Internal server error"));
    }

}
