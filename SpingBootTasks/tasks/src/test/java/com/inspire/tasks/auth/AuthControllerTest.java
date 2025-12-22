package com.inspire.tasks.auth;

import com.inspire.tasks.common.exception.BadRequestException;
import com.inspire.tasks.common.exception.UnauthorizedException;
import com.inspire.tasks.common.exception.GlobalExceptionHandler;
import com.inspire.tasks.common.MessageResponse;
import com.inspire.tasks.auth.jwt.JwtUtils;
import com.inspire.tasks.user.UserDetailsImpl;
import com.inspire.tasks.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.Authentication;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    AuthenticationManager authenticationManager;

    @Mock
    JwtUtils jwtUtils;
    @Mock
    UserService userService;

    @InjectMocks
    AuthController authController;

    private Authentication auth;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        UserDetailsImpl userDetails = new UserDetailsImpl(
                1L,
                "john",
                "john@example.com",
                "encoded-pass",
                null
        );

        auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
    }

    @Test
    void signin_Success() throws Exception {

        when(authenticationManager.authenticate(any())).thenReturn(auth);

        ResponseCookie cookie = ResponseCookie.from("jwt", "token").build();
        when(jwtUtils.generateJwtCookie(any())).thenReturn(cookie);

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                     {"username":"john","password":"123456"}
                """))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.message").value("User logged in successfully!"));
    }

    @Test
    void signin_InvalidCredentials_ThrowsUnauthorized() throws Exception {

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad credentials"));

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                     {"username":"john","password":"wrong"}
                """))
                .andExpect(status().isUnauthorized())
                .andExpect(result ->
                        assertInstanceOf(UnauthorizedException.class, result.getResolvedException()));
    }

    @Test
    void signin_SecondSessionAttempt_ThrowsBadRequest() throws Exception {

        //-- first successful login --
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtUtils.generateJwtCookie(any())).thenReturn(ResponseCookie.from("jwt", "token").build());

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                     {"username":"john","password":"123456"}
                """))
                .andExpect(status().isOk());

        //-- second login should fail --
        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                     {"username":"john","password":"123456"}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertInstanceOf(BadRequestException.class, result.getResolvedException()));
    }

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

    @Test
    void signout_Success() throws Exception {

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtUtils.generateJwtCookie(any())).thenReturn(ResponseCookie.from("jwt", "token").build());

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

    @Test
    void signout_NoActiveSession_ThrowsBadRequest() throws Exception {

        mockMvc.perform(get("/api/auth/signout"))
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertInstanceOf(BadRequestException.class, result.getResolvedException()));
    }
}
