package com.ykleyka.taskboard.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;

class CsrfCookieFilterTest {

    @Test
    void doFilter_whenCsrfTokenExists_forcesTokenCreationAndContinuesChain() throws Exception {
        CsrfCookieFilter filter = new CsrfCookieFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        AtomicInteger tokenReads = new AtomicInteger();
        request.setAttribute(CsrfToken.class.getName(), csrfToken(tokenReads));

        filter.doFilter(request, response, chain);

        assertEquals(1, tokenReads.get());
        assertEquals(request, chain.getRequest());
        assertEquals(response, chain.getResponse());
    }

    private CsrfToken csrfToken(AtomicInteger tokenReads) {
        return new CsrfToken() {
            @Override
            public String getHeaderName() {
                return "X-XSRF-TOKEN";
            }

            @Override
            public String getParameterName() {
                return "_csrf";
            }

            @Override
            public String getToken() {
                tokenReads.incrementAndGet();
                return "token-value";
            }
        };
    }
}
