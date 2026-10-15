package com.fu.cinema;

import com.fu.cinema.dto.ChangePasswordForm;
import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.enums.Role;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.repository.AccountRepository;
import com.fu.cinema.service.AuthService;
import com.fu.cinema.service.CustomerService;
import com.fu.cinema.util.SessionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:customer_password;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class CustomerPasswordTests {
    @Autowired MockMvc mvc;
    @Autowired CustomerService service;
    @Autowired AuthService auth;
    @Autowired AccountRepository accounts;
    @Autowired PasswordEncoder encoder;
    private CustomerProfileFixture.Data data;
    private MockHttpSession session;
    @BeforeEach void seed() {
        data = CustomerProfileFixture.seed(auth);
        session = new MockHttpSession();
        session.setAttribute(SessionUtils.CURRENT_USER, data.an());
    }

    private ChangePasswordForm form(String current, String next, String confirm) {
        var form = new ChangePasswordForm();
        form.setCurrentPassword(current);
        form.setNewPassword(next);
        form.setConfirmPassword(confirm);
        return form;
    }

    @Test
    void changesOnlySessionAccountUsingBcryptAndOldPasswordStopsWorking() throws Exception {
        mvc.perform(post("/customer/password").session(session).with(csrf())
                        .param("currentPassword", "Example@123").param("newPassword", "Changed@456")
                        .param("confirmPassword", "Changed@456").param("accountId", data.binh().getAccountId().toString())
                        .param("customerId", data.binh().getCustomerId().toString()).param("username", "binh@example.test"))
                .andExpect(redirectedUrl("/customer/profile")).andExpect(flash().attribute("success", "Đã đổi mật khẩu"));
        var account = accounts.findById(data.an().getAccountId()).orElseThrow();
        assertThat(account.getPassword()).isNotEqualTo("Changed@456");
        assertThat(encoder.matches("Changed@456", account.getPassword())).isTrue();
        assertThat(auth.login("an@example.test", "Changed@456").getAccountId()).isEqualTo(data.an().getAccountId());
        assertThatThrownBy(() -> auth.login("an@example.test", "Example@123")).isInstanceOf(BusinessException.class);
        assertThat(auth.login("binh@example.test", "Example@123").getAccountId()).isEqualTo(data.binh().getAccountId());
        assertThat(SessionUtils.getCurrentUser(session)).isEqualTo(data.an());
    }

    @Test
    void incorrectCurrentPasswordAndMismatchedConfirmationKeepOldHashAndClearHtmlPasswords() throws Exception {
        String original = accounts.findById(data.an().getAccountId()).orElseThrow().getPassword();
        for (String[] attempt : new String[][]{
                {"Wrong@123", "Changed@456", "Changed@456", "Mật khẩu hiện tại không đúng"},
                {"Example@123", "Changed@456", "Mismatch@456", "Mật khẩu nhập lại không khớp"}}) {
            String html = mvc.perform(post("/customer/password").session(session).with(csrf())
                            .param("currentPassword", attempt[0]).param("newPassword", attempt[1])
                            .param("confirmPassword", attempt[2]))
                    .andExpect(status().isOk()).andExpect(view().name("customer/password"))
                    .andReturn().getResponse().getContentAsString();
            assertThat(html).contains(attempt[3]).doesNotContain(attempt[0], attempt[1], attempt[2]);
            assertThat(accounts.findById(data.an().getAccountId()).orElseThrow().getPassword()).isEqualTo(original);
        }
    }

    @Test
    void blankAndWeakPasswordsHaveFieldErrorsWithoutPersisting() throws Exception {
        mvc.perform(post("/customer/password").session(session).with(csrf()))
                .andExpect(model().attributeHasFieldErrors("form", "currentPassword", "newPassword", "confirmPassword"));
        String html = mvc.perform(post("/customer/password").session(session).with(csrf())
                        .param("currentPassword", "Example@123").param("newPassword", "weak").param("confirmPassword", "weak"))
                .andExpect(model().attributeHasFieldErrors("form", "newPassword"))
                .andReturn().getResponse().getContentAsString();
        assertThat(html).doesNotContain("value=\"weak\"", "value=\"Example@123\"");
        assertThat(auth.login("an@example.test", "Example@123")).isNotNull();
    }

    @Test
    void serviceRejectsPasswordsOverBcryptByteLimitWithoutChangingAccount() {
        String longPassword = "Aa1" + "é".repeat(35);
        assertThatThrownBy(() -> service.changePassword(data.an().getAccountId(),
                form("Example@123", longPassword, longPassword))).isInstanceOf(BusinessException.class)
                .hasMessageContaining("quá dài");
        assertThatThrownBy(() -> service.changePassword(data.an().getAccountId(),
                form("A".repeat(73), "Changed@456", "Changed@456"))).hasMessage("Mật khẩu hiện tại không đúng");
        assertThat(auth.login("an@example.test", "Example@123")).isNotNull();
    }

    @Test
    void credentialsAreNeverTrimmedDuringBinding() throws Exception {
        String newPassword = " Changed@456 ";
        mvc.perform(post("/customer/password").session(session).with(csrf())
                        .param("currentPassword", "Example@123").param("newPassword", newPassword)
                        .param("confirmPassword", newPassword)).andExpect(redirectedUrl("/customer/profile"));
        assertThat(auth.login("an@example.test", newPassword)).isNotNull();
        assertThatThrownBy(() -> auth.login("an@example.test", newPassword.trim())).isInstanceOf(BusinessException.class);
    }

    @Test
    void passwordRequiresCustomerAndCsrfAndFormDoesNotPrepopulateSecrets() throws Exception {
        String html = mvc.perform(get("/customer/password").session(session)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("name=\"_csrf\"", "autocomplete=\"current-password\"", "autocomplete=\"new-password\"");
        mvc.perform(get("/customer/password")).andExpect(redirectedUrl("/login"));
        mvc.perform(post("/customer/password").session(session)).andExpect(status().isForbidden());
        var admin = new MockHttpSession();
        admin.setAttribute(SessionUtils.CURRENT_USER, new SessionUser(-1L, null, "Admin", Role.ADMIN));
        mvc.perform(get("/customer/password").session(admin)).andExpect(status().isForbidden());
        mvc.perform(post("/customer/password").session(admin).with(csrf())).andExpect(status().isForbidden());
    }
}
