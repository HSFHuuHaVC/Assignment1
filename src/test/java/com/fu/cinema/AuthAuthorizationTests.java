package com.fu.cinema;

import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.enums.Role;
import com.fu.cinema.util.SessionUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles({"test", "auth-fixture"})
@AutoConfigureMockMvc
class AuthAuthorizationTests {
    @Autowired MockMvc mvc;

    private MockHttpSession session(Role role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionUtils.CURRENT_USER,
                new SessionUser(1L, role == Role.CUSTOMER ? 2L : null, "An", role));
        return session;
    }

    @Test
    void guestsMustLoginOnBothRootAndNestedProtectedPaths() throws Exception {
        for (String path : new String[]{"/admin", "/admin/movies", "/customer", "/customer/profile"}) {
            mvc.perform(get(path)).andExpect(redirectedUrl("/login"));
        }
        mvc.perform(get("/cinema/admin").contextPath("/cinema")).andExpect(redirectedUrl("/cinema/login"));
    }

    @Test
    void eachRoleCanOnlyReachItsOwnArea() throws Exception {
        for (String path : new String[]{"/admin", "/admin/movies"}) {
            mvc.perform(get(path).session(session(Role.ADMIN))).andExpect(status().isOk());
            mvc.perform(get(path).session(session(Role.CUSTOMER))).andExpect(status().isForbidden());
        }
        for (String path : new String[]{"/customer/__auth-test", "/customer/bookings"}) {
            mvc.perform(get(path).session(session(Role.CUSTOMER))).andExpect(status().isOk());
            mvc.perform(get(path).session(session(Role.ADMIN))).andExpect(status().isForbidden());
        }
    }

    @Test
    void publicAndStaticResourcesStayPublic() throws Exception {
        for (String path : new String[]{"/", "/login", "/register", "/css/app.css"}) {
            mvc.perform(get(path)).andExpect(status().isOk());
        }
    }

    @Test
    void encodedProtectedPathsCannotBypassRoleCheck() throws Exception {
        mvc.perform(get(java.net.URI.create("/%61dmin"))).andExpect(redirectedUrl("/login"));
        mvc.perform(get(java.net.URI.create("/%61dmin")).session(session(Role.CUSTOMER)))
                .andExpect(status().isForbidden());
        mvc.perform(get(java.net.URI.create("/admin%2fmovies"))).andExpect(status().isBadRequest());
        mvc.perform(get("/admin;anything")).andExpect(status().isBadRequest());
    }
}
