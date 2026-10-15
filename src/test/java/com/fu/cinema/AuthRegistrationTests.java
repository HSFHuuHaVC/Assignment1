package com.fu.cinema;

import com.fu.cinema.dto.RegisterForm;
import com.fu.cinema.enums.AccountStatus;
import com.fu.cinema.enums.Role;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.repository.AccountRepository;
import com.fu.cinema.repository.CustomerRepository;
import com.fu.cinema.service.AuthService;
import com.fu.cinema.util.SessionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import java.util.function.Consumer;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:auth_registration;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@ActiveProfiles({"test", "auth-fixture"})
@AutoConfigureMockMvc
class AuthRegistrationTests {
    @Autowired AuthService auth;
    @Autowired AccountRepository accounts;
    @MockitoSpyBean CustomerRepository customers;
    @Autowired PasswordEncoder encoder;
    @Autowired MockMvc mvc;

    @BeforeEach
    void clean() {
        customers.deleteAll();
        accounts.deleteAll();
    }

    private RegisterForm form() {
        RegisterForm form = new RegisterForm();
        form.setFullName("Nguyễn Văn An");
        form.setEmail("an@example.test");
        form.setMobile("0912345678");
        form.setIdentityCard("012345678901");
        form.setBirthday(LocalDate.now().minusYears(13));
        form.setPassword("Private@123");
        form.setConfirmPassword("Private@123");
        return form;
    }

    @Test
    void registrationStoresCustomerAndBcryptAccountAndAllowsLogin() {
        RegisterForm form = form();
        form.setEmail(" AN@EXAMPLE.TEST ");
        var user = auth.register(form);
        var account = accounts.findByUsername("an@example.test").orElseThrow();
        assertThat(account.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.getPassword()).isNotEqualTo(form.getPassword());
        assertThat(encoder.matches(form.getPassword(), account.getPassword())).isTrue();
        assertThat(customers.findByAccount_AccountId(account.getAccountId()).orElseThrow().getCustomerId())
                .isEqualTo(user.getCustomerId());
        assertThat(auth.login("an@example.test", form.getPassword())).isEqualTo(user);
    }

    @Test
    void invalidValuesAreRejectedBeforePersistence() {
        java.util.List<Consumer<RegisterForm>> invalid = java.util.List.of(
                f -> f.setBirthday(LocalDate.now().minusYears(13).plusDays(1)),
                f -> f.setBirthday(LocalDate.now().plusDays(1)),
                f -> f.setMobile("123"),
                f -> f.setIdentityCard("123"),
                f -> f.setEmail("invalid"),
                f -> f.setFullName(" "),
                f -> f.setPassword("weak"),
                f -> f.setConfirmPassword("Different@123"),
                f -> { f.setPassword("Aa1" + "x".repeat(70)); f.setConfirmPassword(f.getPassword()); });
        for (var change : invalid) {
            RegisterForm form = form();
            change.accept(form);
            assertThatThrownBy(() -> auth.register(form)).isInstanceOf(BusinessException.class);
        }
        assertThat(accounts.count()).isZero();
        assertThat(customers.count()).isZero();
    }

    @Test
    void eachUniqueIdentityIsCheckedIndependently() {
        auth.register(form());
        RegisterForm duplicateEmail = form();
        duplicateEmail.setMobile("0987654321");
        duplicateEmail.setIdentityCard("112345678901");
        assertThatThrownBy(() -> auth.register(duplicateEmail)).hasMessageContaining("Email");
        RegisterForm duplicateMobile = form();
        duplicateMobile.setEmail("other@example.test");
        duplicateMobile.setIdentityCard("112345678901");
        assertThatThrownBy(() -> auth.register(duplicateMobile)).hasMessageContaining("điện thoại");
        RegisterForm duplicateIdentity = form();
        duplicateIdentity.setEmail("other@example.test");
        duplicateIdentity.setMobile("0987654321");
        assertThatThrownBy(() -> auth.register(duplicateIdentity)).hasMessageContaining("CCCD");
        assertThat(accounts.count()).isEqualTo(1);
    }

    @Test
    void customerSaveFailureRollsBackTheNewAccount() {
        doThrow(new DataIntegrityViolationException("simulated unique race"))
                .when(customers).saveAndFlush(any());
        try {
            assertThatThrownBy(() -> auth.register(form())).isInstanceOf(BusinessException.class);
            assertThat(accounts.count()).isZero();
            assertThat(customers.count()).isZero();
        } finally {
            reset(customers);
        }
    }

    @Test
    void registerPageAndValidationKeepOnlyNonSensitiveInput() throws Exception {
        mvc.perform(get("/register")).andExpect(status().isOk()).andExpect(view().name("auth/register"));
        String html = mvc.perform(post("/register").with(csrf())
                        .param("fullName", "An").param("email", "an@example.test")
                        .param("mobile", "bad").param("identityCard", "012345678901")
                        .param("birthday", LocalDate.now().minusYears(20).toString())
                        .param("password", "Private@123").param("confirmPassword", "Private@123"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form", "mobile"))
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("an@example.test", "012345678901").doesNotContain("Private@123");
        assertThat(accounts.count()).isZero();
    }

    @Test
    void successfulPostRotatesSessionAndLogsInNewCustomer() throws Exception {
        MockHttpSession old = new MockHttpSession();
        var result = mvc.perform(post("/register").session(old).with(csrf())
                        .param("fullName", "An").param("email", "an@example.test")
                        .param("mobile", "0912345678").param("identityCard", "012345678901")
                        .param("birthday", LocalDate.now().minusYears(13).toString())
                        .param("password", "Private@123").param("confirmPassword", "Private@123"))
                .andExpect(redirectedUrl("/")).andExpect(flash().attribute("success", "Đăng ký thành công"))
                .andReturn();
        assertThat(old.isInvalid()).isTrue();
        var user = SessionUtils.getCurrentUser(result.getRequest().getSession(false));
        assertThat(user).isNotNull();
        assertThat(user.isCustomer()).isTrue();
        assertThat(user.getCustomerId()).isNotNull();
    }
}
