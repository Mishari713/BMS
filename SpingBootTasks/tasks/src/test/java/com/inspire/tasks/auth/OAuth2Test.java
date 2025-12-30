package com.inspire.tasks.auth;

import com.inspire.tasks.auth.jwt.JwtUtils;
import com.inspire.tasks.auth.oauth.OAuth2SuccessHandler;
import com.inspire.tasks.roles.Role;
import com.inspire.tasks.roles.RoleRepository;
import com.inspire.tasks.roles.RoleTypes;
import com.inspire.tasks.user.User;
import com.inspire.tasks.user.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2Test {

    @Mock
    UserRepository userRepository;

    @Mock
    RoleRepository roleRepository;

    @Mock
    JwtUtils jwtUtils;

    @InjectMocks
    OAuth2SuccessHandler successHandler;

    @Test
    void onAuthenticationSuccess_createsUser_setsJwtCookie_andRedirects() throws Exception {

        Role role = new Role();
        role.setName(RoleTypes.ROLE_USER);

        when(roleRepository.findByName(RoleTypes.ROLE_USER))
                .thenReturn(Optional.of(role));

        when(userRepository.findByEmailWithRoles("john@gmail.com"))
                .thenReturn(Optional.empty());

        when(userRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        when(jwtUtils.generateTokenFromUsername(any()))
                .thenReturn("jwt");

        OAuth2User oAuth2User = mock(OAuth2User.class);
        when(oAuth2User.getAttribute("email")).thenReturn("john@gmail.com");
        when(oAuth2User.getAttribute("name")).thenReturn("John Doe");

        OAuth2AuthenticationToken authentication =
                new OAuth2AuthenticationToken(
                        oAuth2User,
                        List.of(),
                        "google"
                );

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(userRepository).save(any(User.class));
        assertThat(response.getCookies()).extracting(Cookie::getName)
                .contains("JWT");
        assertThat(response.getRedirectedUrl())
                .isEqualTo("/swagger-ui/index.html");
    }

    @Test
    void oauth2Login_existingUser_doesNotCreateNewUser() throws Exception {

        User existingUser = new User();
        existingUser.setUsername("john");
        existingUser.setEmail("john@gmail.com");

        when(userRepository.findByEmailWithRoles("john@gmail.com"))
                .thenReturn(Optional.of(existingUser));

        when(jwtUtils.generateTokenFromUsername("john"))
                .thenReturn("jwt");

        OAuth2User oAuth2User = mock(OAuth2User.class);
        when(oAuth2User.getAttribute("email")).thenReturn("john@gmail.com");
        when(oAuth2User.getAttribute("name")).thenReturn("John Doe");

        OAuth2AuthenticationToken authentication =
                new OAuth2AuthenticationToken(
                        oAuth2User,
                        List.of(),
                        "google"
                );

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(userRepository, never()).save(any(User.class));
        verify(jwtUtils).generateTokenFromUsername("john");

        assertThat(response.getRedirectedUrl())
                .isEqualTo("/swagger-ui/index.html");

        assertThat(response.getCookies())
                .extracting(Cookie::getName)
                .contains("JWT");
    }
}
