package com.inspire.tasks.auth;

import com.inspire.tasks.auth.jwt.AuthTokenFilter;
import com.inspire.tasks.common.exception.BadRequestException;
import com.inspire.tasks.common.exception.UnauthorizedException;
import com.inspire.tasks.common.MessageResponse;
import com.inspire.tasks.auth.jwt.JwtUtils;
import com.inspire.tasks.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.Authentication;

import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = AuthTokenFilter.class
        )
)
@Import(TestSecurityConfig.class)
class AuthControllerTest {

    @MockitoBean
    AuthenticationManager authenticationManager;

    @MockitoBean
    JwtUtils jwtUtils;

    @MockitoBean
    UserService userService;

    @MockitoBean
    UserRepository userRepository;

    @Autowired
    MockMvc mockMvc;

    private Authentication auth;

    private User user;

    @BeforeEach
    void setup() {

        UserDetailsImpl userDetails = new UserDetailsImpl(
                1L,
                "john",
                "john@example.com",
                "encoded-pass",
                null
        );

        user = new User(userDetails.getUsername(),userDetails.getEmail(), userDetails.getPassword());

        user.setProvider(AuthProvider.LOCAL);

        auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
    }

    @WithMockUser
    @Test
    void signin_Success() throws Exception {

        when(authenticationManager.authenticate(any())).thenReturn(auth);

        ResponseCookie cookie = ResponseCookie.from("jwt", "token").build();
        when(jwtUtils.generateJwtCookie(any())).thenReturn(cookie);

        when(userRepository.findByUsername("john"))
                .thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                     {"username":"john","password":"123456"}
                """))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.message").value("User logged in successfully!"));
    }

    @WithMockUser
    @Test
    void signin_InvalidCredentials_ThrowsUnauthorized() throws Exception {

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad credentials"));
        when(userRepository.findByUsername("john"))
                .thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                     {"username":"john","password":"wrong"}
                """))
                .andExpect(status().isUnauthorized())
                .andExpect(result ->
                        assertInstanceOf(UnauthorizedException.class, result.getResolvedException()));
    }

    @WithMockUser
    @Test
    void signin_SecondSessionAttempt_ThrowsBadRequest() throws Exception {

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtUtils.generateJwtCookie(any())).thenReturn(ResponseCookie.from("jwt", "token").build());
        when(userRepository.findByUsername("john"))
                .thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                     {"username":"john","password":"123456"}
                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                     {"username":"john","password":"123456"}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertInstanceOf(BadRequestException.class, result.getResolvedException()));
    }

    @WithMockUser
    @Test
    void signup_Success() throws Exception {

        when(userService.createUser(any()))
                .thenAnswer(invocation -> ResponseEntity.ok(
                        new MessageResponse(200, "User registered successfully!")
                ));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                     {"username":"john","email":"john@example.com","password":"123456"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully!"));
    }

    @WithMockUser
    @Test
    void signout_Success() throws Exception {

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtUtils.generateJwtCookie(any())).thenReturn(ResponseCookie.from("jwt", "token").build());
        when(userRepository.findByUsername("john"));

        mockMvc.perform(post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                     {"username":"john","password":"123456"}
                """));

        when(jwtUtils.getCleanJwtCookie()).thenReturn(ResponseCookie.from("jwt", "").build());

        mockMvc.perform(get("/api/auth/signout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("You've been signed out successfully!"));
    }

    @WithMockUser
    @Test
    void signout_NoActiveSession_ThrowsBadRequest() throws Exception {

        mockMvc.perform(get("/api/auth/signout"))
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertInstanceOf(BadRequestException.class, result.getResolvedException()));
    }
}
