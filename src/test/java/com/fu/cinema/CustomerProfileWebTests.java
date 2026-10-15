package com.fu.cinema;

import com.fu.cinema.dto.ProfileForm;
import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.enums.Role;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.service.AuthService;
import com.fu.cinema.service.CustomerService;
import com.fu.cinema.util.SessionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:profile_web;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class CustomerProfileWebTests {
    @Autowired MockMvc mvc;
    @Autowired AuthService auth;
    @Autowired CustomerService service;
    private CustomerProfileFixture.Data data;
    private MockHttpSession session;
    @BeforeEach void seed() {
        data = CustomerProfileFixture.seed(auth);
        session = new MockHttpSession();
        session.setAttribute(SessionUtils.CURRENT_USER, data.an());
    }

    private MockHttpServletRequestBuilder submit(ProfileForm form) {
        return post("/customer/profile").session(session).with(csrf())
                .param("fullName", form.getFullName()).param("email", form.getEmail()).param("mobile", form.getMobile())
                .param("birthday", form.getBirthday().toString()).param("identityCard", form.getIdentityCard());
    }

    @Test
    void formShowsOwnDisabledUsernameAndAllowsSavingUnchangedProfile() throws Exception {
        String html = mvc.perform(get("/customer/profile").session(session)).andExpect(status().isOk())
                .andExpect(view().name("customer/profile")).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("an@example.test", "Nguyen Van An", "name=\"_csrf\"")
                .containsPattern("id=\"username\"[^>]*disabled");
        mvc.perform(submit(service.getProfile(data.an().getCustomerId())))
                .andExpect(redirectedUrl("/customer/profile")).andExpect(flash().attribute("success", "Đã cập nhật hồ sơ"));
    }

    @Test
    void saveRefreshesSessionMenuButCannotChangeUsernameOrOtherCustomer() throws Exception {
        var form = service.getProfile(data.an().getCustomerId());
        form.setFullName("  New Name  ");
        form.setEmail(" CONTACT@EXAMPLE.TEST ");
        mvc.perform(submit(form).param("username", "hacked@example.test")
                        .param("customerId", data.binh().getCustomerId().toString())
                        .param("accountId", data.binh().getAccountId().toString()).param("role", "ADMIN").param("status", "LOCKED"))
                .andExpect(redirectedUrl("/customer/profile"));
        SessionUser current = SessionUtils.getCurrentUser(session);
        assertThat(current.getDisplayName()).isEqualTo("New Name");
        assertThat(current.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(current.getCustomerId()).isEqualTo(data.an().getCustomerId());
        assertThat(current.getAccountId()).isEqualTo(data.an().getAccountId());
        assertThat(service.getProfile(data.an().getCustomerId()).getUsername()).isEqualTo("an@example.test");
        assertThat(service.getProfile(data.an().getCustomerId()).getEmail()).isEqualTo("contact@example.test");
        assertThat(service.getProfile(data.binh().getCustomerId()).getFullName()).isEqualTo("Tran Thi Binh");
        assertThat(auth.login("an@example.test", "Example@123").getDisplayName()).isEqualTo("New Name");
        assertThatThrownBy(() -> auth.login("contact@example.test", "Example@123")).isInstanceOf(BusinessException.class);
        String html = mvc.perform(get("/customer/profile").session(session)).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Xin chào, New Name");
    }

    @Test
    void validationRestoresTrustedUsernameAndPreservesInputWithoutChangingSession() throws Exception {
        var form = service.getProfile(data.an().getCustomerId());
        form.setFullName("Attempt");
        form.setMobile("bad");
        String html = mvc.perform(submit(form).param("username", "fake@example.test"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form", "mobile"))
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("an@example.test", "Attempt", "Số điện thoại gồm").doesNotContain("fake@example.test");
        assertThat(SessionUtils.getCurrentUser(session)).isEqualTo(data.an());
        assertThat(service.getProfile(data.an().getCustomerId()).getFullName()).isEqualTo("Nguyen Van An");
    }

    @Test
    void duplicateMobileShowsErrorAndDoesNotRefreshSessionName() throws Exception {
        var form = service.getProfile(data.an().getCustomerId());
        form.setFullName("Attempt");
        form.setMobile("0912345678");
        String html = mvc.perform(submit(form)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Số điện thoại đã được sử dụng");
        assertThat(SessionUtils.getCurrentUser(session)).isEqualTo(data.an());
    }

    @Test
    void profileRequiresCustomerAndCsrfAndRejectsMalformedSession() throws Exception {
        mvc.perform(get("/customer/profile")).andExpect(redirectedUrl("/login"));
        mvc.perform(post("/customer/profile").session(session)).andExpect(status().isForbidden());
        var admin = new MockHttpSession();
        admin.setAttribute(SessionUtils.CURRENT_USER, new SessionUser(-1L, null, "Admin", Role.ADMIN));
        mvc.perform(get("/customer/profile").session(admin)).andExpect(status().isForbidden());
        mvc.perform(submit(service.getProfile(data.an().getCustomerId())).session(admin)).andExpect(status().isForbidden());
        var malformed = new MockHttpSession();
        malformed.setAttribute(SessionUtils.CURRENT_USER, new SessionUser(-1L, null, "An", Role.CUSTOMER));
        mvc.perform(get("/customer/profile").session(malformed)).andExpect(status().isForbidden());
    }
}
