package com.ykleyka.taskboard.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.ykleyka.taskboard.service.AuthService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.web.csrf.DefaultCsrfToken;

class AuthControllerTest {

    @Test
    void csrf_returnsTokenMetadataForSpaClient() {
        AuthController controller = new AuthController(mock(AuthService.class));
        DefaultCsrfToken csrfToken = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "token-value");

        Map<String, String> response = controller.csrf(csrfToken);

        assertEquals("X-XSRF-TOKEN", response.get("headerName"));
        assertEquals("_csrf", response.get("parameterName"));
        assertEquals("token-value", response.get("token"));
    }
}
