package com.inspire.tasks.user;

import com.inspire.tasks.common.exception.BadRequestException;
import com.inspire.tasks.auth.dto.SignupRequest;
import com.inspire.tasks.common.MessageResponse;
import com.inspire.tasks.roles.Role;
import com.inspire.tasks.roles.RoleRepository;
import com.inspire.tasks.roles.RoleTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder encoder;

    @InjectMocks
    private UserService userService;

    private User user;
    private SignupRequest signupRequest;

    private Role userRole;
    private Role authRole;
    private Role adminRole;

    @BeforeEach
    void setup() {
        signupRequest = new SignupRequest();
        signupRequest.setUsername("John");
        signupRequest.setEmail("john@example.com");
        signupRequest.setPassword("12345password");
        signupRequest.setRole(null); // default

        userRole = new Role(RoleTypes.ROLE_USER);
        authRole = new Role(RoleTypes.ROLE_AUTHOR);
        adminRole = new Role(RoleTypes.ROLE_ADMIN);

        user = new User();
        user.setId(1L);
        user.setUsername("john");
        user.setEmail("john@example.com");
        user.setPassword("encoded-pass");
        user.setRoles(Set.of(userRole));
    }

    // Test default ROLE_USER
    @Test
    void createUser_DefaultUserRole_ReturnsSuccess() {
        signupRequest.setRole(null);

        when(userRepository.existsByUsername("John")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(encoder.encode("12345password")).thenReturn("encoded-pass");
        when(roleRepository.findByName(RoleTypes.ROLE_USER)).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        ResponseEntity<?> response = userService.createUser(signupRequest);

        assertEquals(200, response.getStatusCodeValue());
        MessageResponse body = (MessageResponse) response.getBody();
        assertEquals("User registered successfully!", body.getMessage());
        assertEquals(200, body.getCode());

        verify(roleRepository).findByName(RoleTypes.ROLE_USER);
    }

    // Test ROLE_AUTHOR
    @Test
    void createUser_AuthorRole_ReturnsSuccess() {
        signupRequest.setRole(Set.of("author"));

        when(userRepository.existsByUsername("John")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(encoder.encode("12345password")).thenReturn("encoded-pass");
        when(roleRepository.findByName(RoleTypes.ROLE_AUTHOR)).thenReturn(Optional.of(authRole));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        ResponseEntity<?> response = userService.createUser(signupRequest);

        assertEquals(200, response.getStatusCodeValue());
        MessageResponse body = (MessageResponse) response.getBody();
        assertEquals("User registered successfully!", body.getMessage());
        assertTrue(body.getMessage().contains("successfully"));
        verify(roleRepository).findByName(RoleTypes.ROLE_AUTHOR);
    }

    // Test ROLE_ADMIN
    @Test
    void createUser_AdminRole_ReturnsSuccess() {
        signupRequest.setRole(Set.of("admin"));

        when(userRepository.existsByUsername("John")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(encoder.encode("12345password")).thenReturn("encoded-pass");
        when(roleRepository.findByName(RoleTypes.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        ResponseEntity<?> response = userService.createUser(signupRequest);

        assertEquals(200, response.getStatusCodeValue());
        MessageResponse body = (MessageResponse) response.getBody();
        assertEquals("User registered successfully!", body.getMessage());
        verify(roleRepository).findByName(RoleTypes.ROLE_ADMIN);
    }

@Test
    void createUser_UsernameExists_ThrowsException() {

        signupRequest.setUsername("John");
        signupRequest.setEmail("john@example.com");

        when(userRepository.existsByUsername("John")).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> userService.createUser(signupRequest));

        assertEquals("Error: Username is already taken!", ex.getMessage());
    }

    @Test
    void createUser_EmailExists_ThrowsException() {

        signupRequest.setUsername("John");
        signupRequest.setEmail("john@example.com");

        when(userRepository.existsByUsername("John")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> userService.createUser(signupRequest));

        assertEquals("Error: Email is already in use!", ex.getMessage());
    }

    @Test
    void findById_ReturnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User found = userService.findById(1L);
        assertEquals(user.getUsername(), found.getUsername());
    }

    @Test
    void findById_NotFound_ThrowsException() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> userService.findById(2L));
        assertTrue(ex.getMessage().contains("User id: 2"));
    }

    @Test
    void deleteById_Success() {
        doNothing().when(userRepository).deleteById(1L);

        ResponseEntity<?> response = userService.deleteById(1L);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof MessageResponse);
        MessageResponse body = (MessageResponse) response.getBody();
        assertEquals("User has been deleted successfully!", body.getMessage());

        verify(userRepository).deleteById(1L);
    }

    @Test
    void saveUser_Success() {
        when(userRepository.save(any(User.class))).thenReturn(user);

        ResponseEntity<?> response = userService.save(user);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("User updated successfully!", ((MessageResponse) response.getBody()).getMessage());

        verify(userRepository).save(user);
    }

    @Test
    void findByUsername_ReturnsUser() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        User found = userService.findByUsername("john");
        assertEquals(user.getUsername(), found.getUsername());
    }

    @Test
    void findByUsername_NotFound_ThrowsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> userService.findByUsername("unknown"));
        assertTrue(ex.getMessage().contains("Username: unknown"));
    }

    @Test
    void existsByUsername_ReturnsTrue() {
        when(userRepository.existsByUsername("john")).thenReturn(true);
        assertTrue(userService.existsByUsername("john"));
    }

    @Test
    void existsByEmail_ReturnsFalse() {
        when(userRepository.existsByEmail("someone@example.com")).thenReturn(false);
        assertFalse(userService.existsByEmail("someone@example.com"));
    }

    @Test
    void findAll_ReturnsUsers() {
        List<User> users = List.of(user);
        when(userRepository.findAll()).thenReturn(users);

        List<User> result = userService.findAll();
        assertEquals(1, result.size());
        assertEquals(user.getUsername(), result.get(0).getUsername());
    }
}
