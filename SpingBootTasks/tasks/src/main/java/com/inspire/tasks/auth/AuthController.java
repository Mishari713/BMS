package com.inspire.tasks.auth;

import com.inspire.tasks.common.exception.BadRequestException;
import com.inspire.tasks.common.exception.UnauthorizedException;
import com.inspire.tasks.roles.RoleRepository;
import com.inspire.tasks.auth.dto.LoginRequest;
import com.inspire.tasks.auth.dto.SignupRequest;
import com.inspire.tasks.common.MessageResponse;
import com.inspire.tasks.user.*;
import com.inspire.tasks.auth.jwt.JwtUtils;
import jakarta.validation.Valid;

import lombok.extern.slf4j.Slf4j;
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
        User user = userRepository.findByUsername(loginRequest.getUsername()).
                orElseThrow(() -> new BadRequestException("user not found"));

        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new BadRequestException(
                    "This account uses OAuth2 login. Please sign in with Google."
            );
        }
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