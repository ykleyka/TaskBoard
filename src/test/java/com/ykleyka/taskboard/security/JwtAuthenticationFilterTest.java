package com.ykleyka.taskboard.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ykleyka.taskboard.model.User;
import com.ykleyka.taskboard.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
    @Mock
    private JwtTokenService tokenService;
    @Mock
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_whenAuthorizationHeaderMissing_skipsAuthentication() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenService, userRepository);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(request, chain.getRequest());
        verify(tokenService, never()).parse(anyString());
    }

    @Test
    void doFilter_whenHeaderIsNotBearer_skipsAuthentication() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenService, userRepository);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic abc");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(tokenService, never()).parse(anyString());
    }

    @Test
    void doFilter_whenBearerTokenIsBlank_skipsAuthentication() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenService, userRepository);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer   ");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(tokenService, never()).parse(anyString());
    }

    @Test
    void doFilter_whenTokenIsValid_setsAuthenticatedUserPrincipal() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenService, userRepository);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-value");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        User user = user(10L, "alice");

        when(tokenService.parse("token-value"))
                .thenReturn(new TokenClaims(10L, Instant.parse("2030-01-01T00:00:00Z")));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        filter.doFilter(request, response, chain);

        UsernamePasswordAuthenticationToken authentication =
                assertInstanceOf(
                        UsernamePasswordAuthenticationToken.class,
                        SecurityContextHolder.getContext().getAuthentication());
        AuthenticatedUser principal =
                assertInstanceOf(AuthenticatedUser.class, authentication.getPrincipal());
        assertEquals(10L, principal.id());
        assertEquals("alice", principal.username());
        assertEquals("token-value", authentication.getCredentials());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_USER")));
        assertEquals(request, chain.getRequest());
    }

    @Test
    void doFilter_whenTokenIsInvalid_clearsContextAndContinuesChain() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenService, userRepository);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(tokenService.parse("bad-token")).thenThrow(new BadCredentialsException("bad"));

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(request, chain.getRequest());
    }

    @Test
    void doFilter_whenUserFromTokenDoesNotExist_clearsContext() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenService, userRepository);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-value");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(tokenService.parse("token-value"))
                .thenReturn(new TokenClaims(404L, Instant.parse("2030-01-01T00:00:00Z")));
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilter_whenAuthenticationAlreadyExists_doesNotParseToken() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenService, userRepository);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-value");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("existing", "credentials"));

        filter.doFilter(request, response, chain);

        assertEquals("existing", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(tokenService, never()).parse(anyString());
    }

    @Test
    void authenticatedUserFrom_mapsUserFields() {
        User user = user(20L, "mapped");
        user.setEmail("mapped@example.com");
        user.setFirstName("Map");
        user.setLastName("Ped");

        AuthenticatedUser principal = AuthenticatedUser.from(user);

        assertEquals(20L, principal.id());
        assertEquals("mapped", principal.username());
        assertEquals("mapped@example.com", principal.email());
        assertEquals("Map", principal.firstName());
        assertEquals("Ped", principal.lastName());
    }

    private User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }
}
