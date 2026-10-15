package com.fu.cinema;

import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.enums.Role;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.service.AuthService;
import com.fu.cinema.util.SessionUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AuthLoginTests {
    @Autowired MockMvc mvc;
    @MockitoBean AuthService auth;

    @Test
    void loginPageIsOurThymeleafFormWithCsrfAndGuestMenu() throws Exception {
        String html = mvc.perform(get("/login")).andExpect(status().isOk())
                .andExpect(view().name("auth/login")).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Đăng nhập | FU Cinema", "name=\"_csrf\"", "Đăng ký", "FU Cinema Ticket System")
                .doesNotContain("Please sign in");
    }

    @Test
    void successfulLoginInvalidatesOldSessionAndRedirectsAdmin() throws Exception {
        SessionUser user = new SessionUser(7L, null, "Administrator", Role.ADMIN);
        when(auth.login("admin@example.test", "Example@123")).thenReturn(user);
        MockHttpSession old = new MockHttpSession();
        old.setAttribute("old-private-state", "discard-me");
        var result = mvc.perform(post("/login").session(old).with(csrf())
                        .param("username", "admin@example.test").param("password", "Example@123"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin")).andReturn();
        assertThat(old.isInvalid()).isTrue();
        var session = result.getRequest().getSession(false);
        assertThat(session).isNotNull().isNotSameAs(old);
        assertThat(session.getAttribute(SessionUtils.CURRENT_USER)).isEqualTo(user);
        assertThat(session.getAttribute("old-private-state")).isNull();
    }

    @Test
    void successfulCustomerLoginRedirectsHome() throws Exception {
        when(auth.login("customer@example.test", "Example@123"))
                .thenReturn(new SessionUser(7L, 8L, "An", Role.CUSTOMER));
        mvc.perform(post("/login").with(csrf()).param("username", "customer@example.test")
                        .param("password", "Example@123"))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void loginFailureKeepsUsernameButNeverReflectsPassword() throws Exception {
        when(auth.login("user@example.test", "Private@123"))
                .thenThrow(new BusinessException("Tên đăng nhập hoặc mật khẩu không đúng"));
        String html = mvc.perform(post("/login").with(csrf()).param("username", "user@example.test")
                        .param("password", "Private@123"))
                .andExpect(status().isOk()).andExpect(view().name("auth/login"))
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("user@example.test", "Tên đăng nhập hoặc mật khẩu không đúng")
                .doesNotContain("Private@123");
    }

    @Test
    void invalidFormDoesNotCallService() throws Exception {
        mvc.perform(post("/login").with(csrf()).param("username", " ").param("password", ""))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form", "username", "password"));
        verifyNoInteractions(auth);
    }

    @Test
    void logoutInvalidatesAuthenticatedSessionAndFlashesSuccess() throws Exception {
        MockHttpSession old = new MockHttpSession();
        old.setAttribute(SessionUtils.CURRENT_USER, new SessionUser(1L, 2L, "An", Role.CUSTOMER));
        var result = mvc.perform(post("/logout").session(old).with(csrf()))
                .andExpect(redirectedUrl("/login")).andExpect(flash().attribute("success", "Đã đăng xuất"))
                .andReturn();
        assertThat(old.isInvalid()).isTrue();
        assertThat(SessionUtils.getCurrentUser(result.getRequest().getSession(false))).isNull();
    }

    @Test
    void logoutRequiresPostAndMutatingRequestsRequireCsrf() throws Exception {
        mvc.perform(get("/logout")).andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/login").param("username", "x").param("password", "y"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/logout")).andExpect(status().isForbidden());
        verifyNoInteractions(auth);
    }
}
