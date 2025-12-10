package com.inspire.tasks.user;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.inspire.tasks.exception.BadRequestException;
import com.inspire.tasks.payload.request.SignupRequest;
import com.inspire.tasks.roles.Role;
import com.inspire.tasks.roles.RoleRepository;
import com.inspire.tasks.roles.RoleTypes;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Set;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin")
public class AdminRestController {

    PasswordEncoder encoder;

    RoleRepository roleRepository;

    UserService userService;

    ObjectMapper objectMapper;

    public AdminRestController(UserService userService, ObjectMapper objectMapper, RoleRepository roleRepository, PasswordEncoder encoder){
        this.userService = userService;
        this.objectMapper = objectMapper;
        this.encoder = encoder;
        this.roleRepository = roleRepository;
    }

    @PostConstruct
    public void createAdmin(){
        if (Boolean.getBoolean("skipPostConstruct")) return; // skip this method if testing

        // Create new user's account

        if (!userService.existsByUsername("systemAdmin") && !userService.existsByEmail("admin.1@email.com")) {
            User user = new User("systemAdmin1",
                    "admin.1@email.com",
                    encoder.encode("adminPass"));
            Role adminRole = roleRepository.findByName(RoleTypes.ROLE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));

            user.setRoles(Set.of(adminRole));
            userService.save(user);
        }
    }


    @GetMapping("/users")
    public List<User> findAll() {

        return userService.findAll();
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@Valid @RequestBody SignupRequest signUpRequest){
        return userService.createUser(signUpRequest);
    }

    @PatchMapping("/users/{userId}")
    public ResponseEntity<?> updateUser(@PathVariable Long userId,
                                         @RequestBody Map<String, Object> patchPayload){

        User tempUser = userService.findById(userId);

        // throw exception if request body contains "id" key
        if(patchPayload.containsKey("id")){
            throw new BadRequestException("User id is not allowed in request body - " + userId);
        }

        return userService.save(apply(patchPayload, tempUser));
    }

    User apply(Map<String, Object> patchPayload, User user) {
        ObjectNode userNode = objectMapper.convertValue(user, ObjectNode.class);
        ObjectNode patchNode = objectMapper.convertValue(patchPayload, ObjectNode.class);
        userNode.setAll(patchNode);
        return objectMapper.convertValue(userNode, User.class);
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId){

        User user = userService.findById(userId);
         return userService.deleteById(user.getId());
    }

    // Pagination usage
//    @GetMapping("/lastUsers")
//    public Page<User> getLastUsers(){
//        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").descending());
//
//        return userRepository.findAll(pageable);
//    }
}