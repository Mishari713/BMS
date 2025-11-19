package com.inspire.tasks.security;

import com.inspire.tasks.exception.BadRequestException;
import com.inspire.tasks.exception.UnauthorizedException;
import com.inspire.tasks.roles.RoleRepository;
import com.inspire.tasks.payload.request.LoginRequest;
import com.inspire.tasks.payload.request.SignupRequest;
import com.inspire.tasks.payload.response.MessageResponse;
import com.inspire.tasks.user.UserRepository;
import com.inspire.tasks.security.jwt.JwtUtils;
import com.inspire.tasks.user.UserDetailsImpl;
import com.inspire.tasks.user.UserService;
import jakarta.validation.Valid;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private boolean activeSession;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    UserService userService;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        if(!activeSession) {
            try {
                Authentication authentication = authenticationManager
                        .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

                SecurityContextHolder.getContext().setAuthentication(authentication);

                UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

                ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(userDetails);

                activeSession = true;
                log.info("User: {} logged in", loginRequest.getUsername());
                return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                        .body(new MessageResponse(200, "User logged in successfully!"));
            } catch (BadCredentialsException e) {
                throw new UnauthorizedException("Invalid username or password");
            }
        }
        else{
            throw new BadRequestException("Error: Only 1 active session is allowed");
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        return userService.createUser(signUpRequest);
    }

    @GetMapping("/signout")
    public ResponseEntity<?> logoutUser() {
        if(activeSession) {
            activeSession = false;
            ResponseCookie cookie = jwtUtils.getCleanJwtCookie();
            log.info("Current user has signed out");
            return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(new MessageResponse(200, "You've been signed out successfully!"));
        }
        else {
            throw new BadRequestException("Error: No active session detected");
        }
    }
}