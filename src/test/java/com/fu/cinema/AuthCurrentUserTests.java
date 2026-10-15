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
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles({"test", "auth-fixture"})
@AutoConfigureMockMvc
class AuthCurrentUserTests {
    @Autowired MockMvc mvc;

    @Test
    void publicViewRendersGuestAndEachRoleFromSession() throws Exception {
        String guest = mvc.perform(get("/")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(guest).contains("Đăng nhập", "Đăng ký").doesNotContain("Dashboard", "Vé của tôi");
        for (Role role : Role.values()) {
            MockHttpSession session = new MockHttpSession();
            SessionUser user = new SessionUser(1L, role == Role.CUSTOMER ? 2L : null, "<An>", role);
            session.setAttribute(SessionUtils.CURRENT_USER, user);
            String html = mvc.perform(get("/").session(session))
                    .andExpect(model().attribute("currentUser", user))
                    .andReturn().getResponse().getContentAsString();
            assertThat(html).contains("&lt;An&gt;", "Đăng xuất", "name=\"_csrf\"");
            if (role == Role.ADMIN) assertThat(html).contains("Dashboard").doesNotContain("Vé của tôi");
            else assertThat(html).contains("Vé của tôi").doesNotContain("Dashboard");
        }
    }
}
