package com.ykleyka.taskboard.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.csrf.DefaultCsrfToken;

class SpaCsrfTokenRequestHandlerTest {

    @Test
    void resolveCsrfTokenValue_whenHeaderContainsRawCookieToken_returnsHeaderValue() {
        SpaCsrfTokenRequestHandler handler = new SpaCsrfTokenRequestHandler();
        DefaultCsrfToken csrfToken = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "raw-token");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-XSRF-TOKEN", "raw-token");

        String resolved = handler.resolveCsrfTokenValue(request, csrfToken);

        assertEquals("raw-token", resolved);
    }
}
