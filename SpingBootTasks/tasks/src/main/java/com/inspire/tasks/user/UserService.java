package com.inspire.tasks.user;

import com.inspire.tasks.exception.BadRequestException;
import com.inspire.tasks.payload.request.SignupRequest;
import com.inspire.tasks.payload.response.MessageResponse;
import com.inspire.tasks.roles.Role;
import com.inspire.tasks.roles.RoleRepository;
import com.inspire.tasks.roles.RoleTypes;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserService {

    UserRepository userRepository;

    RoleRepository roleRepository;

    PasswordEncoder encoder;

    UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder encoder){
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.encoder = encoder;
    }

    public ResponseEntity<?> createUser(@Valid @RequestBody SignupRequest signUpRequest){
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            throw new BadRequestException("Error: Username is already taken!");
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new BadRequestException("Error: Email is already in use!");
        }

        // Create new user's account
        User user = new User(signUpRequest.getUsername().toLowerCase(),
                signUpRequest.getEmail().toLowerCase(),
                encoder.encode(signUpRequest.getPassword()));

        Set<String> strRoles = signUpRequest.getRole();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null) {
            Role userRole = roleRepository.findByName(RoleTypes.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(userRole);
        } else {
            strRoles = signUpRequest.getRole().stream().map(String::toLowerCase).collect(Collectors.toSet());
            strRoles.forEach(role -> {
                switch (role) {
                    case "admin":
                        Role adminRole = roleRepository.findByName(RoleTypes.ROLE_ADMIN)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(adminRole);

                        break;
                    case "author":
                        Role authorRole = roleRepository.findByName(RoleTypes.ROLE_AUTHOR)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(authorRole);

                        break;
                    default:
                        Role userRole = roleRepository.findByName(RoleTypes.ROLE_USER)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(userRole);
                }
            });
        }

        user.setRoles(roles);
        userRepository.save(user);

        log.info("User signup request with username {}", signUpRequest.getUsername());

        return ResponseEntity.ok(new MessageResponse(200, "User registered successfully!"));
    }

    public ResponseEntity<?> save(User user) {
        userRepository.save(user);
        return ResponseEntity.ok(new MessageResponse(200, "User updated successfully!"));
    }

    User findById(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> {
                log.warn("User with id {} not found", userId);
        return new BadRequestException("User id: " + userId + " doesn't exists");
        });
    }

    public ResponseEntity<?> deleteById(Long userId) {
        log.info("Deleting user with id {}", userId);
        userRepository.deleteById(userId);
        return ResponseEntity.ok(new MessageResponse(200, "User has been deleted successfully!"));
    }


    public User findByUsername(String username){
        return userRepository.findByUsername(username).orElseThrow(() -> {
                log.warn("User with username {} not found", username);
               return new BadRequestException("Username: " + username + " doesn't exists");
        });
    }

    boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    List<User> findAll() {
        return userRepository.findAll();
    }
}
