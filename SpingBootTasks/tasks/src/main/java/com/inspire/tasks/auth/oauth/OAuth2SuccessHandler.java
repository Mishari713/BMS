package com.inspire.tasks.auth.oauth;

import com.inspire.tasks.auth.jwt.JwtUtils;
import com.inspire.tasks.roles.Role;
import com.inspire.tasks.roles.RoleRepository;
import com.inspire.tasks.roles.RoleTypes;
import com.inspire.tasks.user.AuthProvider;
import com.inspire.tasks.user.User;
import com.inspire.tasks.user.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    UserRepository userRepository;
    RoleRepository roleRepository;
    JwtUtils jwtUtils;

    public OAuth2SuccessHandler(UserRepository userRepository, RoleRepository roleRepository, JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.jwtUtils = jwtUtils;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String provider = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        User user = userRepository.findByEmailWithRoles(email)
                .orElseGet(() -> createNewUser(email, name, provider));

        String jwt = jwtUtils.generateTokenFromUsername(user.getUsername());

        Cookie jwtCookie = new Cookie("JWT", jwt);
        jwtCookie.setHttpOnly(false); // must be accessible by JS
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(3600); // 1 hour
        response.addCookie(jwtCookie);

        response.sendRedirect("/swagger-ui/index.html");

    }

    private User createNewUser(String email, String name, String provider) {
        Role userRole = roleRepository.findByName(RoleTypes.ROLE_USER)
                .orElseThrow();
//        if (email.startsWith(""))
        User user = new User();
        user.setEmail(email);
        user.setUsername(name);
        user.setPassword("OAUTH2");
        user.setProvider(AuthProvider.valueOf(provider.toUpperCase()));
        user.setRoles(Set.of(userRole));

        return userRepository.save(user);
    }
}
