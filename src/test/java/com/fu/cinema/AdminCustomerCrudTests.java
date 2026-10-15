package com.fu.cinema;

import com.fu.cinema.dto.CustomerForm;
import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.entity.Account;
import com.fu.cinema.enums.AccountStatus;
import com.fu.cinema.enums.Role;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.repository.AccountRepository;
import com.fu.cinema.repository.CustomerRepository;
import com.fu.cinema.service.AuthService;
import com.fu.cinema.service.CustomerService;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:customer_crud;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AdminCustomerCrudTests {
    @Autowired MockMvc mvc;
    @Autowired CustomerService service;
    @Autowired AuthService auth;
    @Autowired AccountRepository accounts;
    @MockitoSpyBean CustomerRepository customers;
    @Autowired PasswordEncoder encoder;

    @BeforeEach
    void clean() {
        customers.deleteAll();
        accounts.deleteAll();
    }

    private CustomerForm form(int number) {
        CustomerForm form = new CustomerForm();
        form.setFullName("Customer " + number);
        form.setEmail("customer" + number + "@example.test");
        form.setMobile(String.format("09%08d", number));
        form.setIdentityCard(String.format("%012d", number));
        form.setBirthday(LocalDate.now().minusYears(13));
        form.setPassword("Example@123");
        return form;
    }

    private MockHttpSession admin() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionUtils.CURRENT_USER, new SessionUser(-1L, null, "Admin", Role.ADMIN));
        return session;
    }

    private MockHttpServletRequestBuilder postForm(String url, CustomerForm form) {
        return post(url).session(admin()).with(csrf()).param("fullName", form.getFullName())
                .param("email", form.getEmail()).param("mobile", form.getMobile())
                .param("birthday", form.getBirthday().toString()).param("identityCard", form.getIdentityCard())
                .param("password", form.getPassword()).param("status", form.getStatus().name());
    }

    private Long createCustomer(int number) {
        service.create(form(number));
        return customers.findByAccount_AccountId(accounts.findByUsername(form(number).getEmail())
                .orElseThrow().getAccountId()).orElseThrow().getCustomerId();
    }

    @Test
    void createPostNormalizesIdentityStoresBcryptAndRedirects() throws Exception {
        CustomerForm form = form(1);
        form.setEmail(" CUSTOMER1@EXAMPLE.TEST ");
        form.setFullName("  Customer One  ");
        mvc.perform(postForm("/admin/customers/new", form).param("customerId", "999"))
                .andExpect(redirectedUrl("/admin/customers"))
                .andExpect(flash().attribute("success", "Đã thêm khách hàng Customer One"));
        var account = accounts.findByUsername("customer1@example.test").orElseThrow();
        assertThat(encoder.matches("Example@123", account.getPassword())).isTrue();
        assertThat(account.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(customers.findByAccount_AccountId(account.getAccountId()).orElseThrow().getFullName())
                .isEqualTo("Customer One");
    }

    @Test
    void fieldsShowValidationErrorsAndPasswordsAreNeverReflected() throws Exception {
        CustomerForm invalid = form(1);
        invalid.setMobile("bad");
        invalid.setPassword("Private@123");
        String html = mvc.perform(postForm("/admin/customers/new", invalid))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form", "mobile"))
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("customer1@example.test", "is-invalid", "Số điện thoại gồm")
                .doesNotContain("Private@123");
        invalid = form(1);
        invalid.setPassword("");
        mvc.perform(postForm("/admin/customers/new", invalid))
                .andExpect(model().attributeHasFieldErrors("form", "password"));
        assertThat(accounts.count()).isZero();
    }

    @Test
    void duplicatesExcludeCurrentCustomerButRejectOtherCustomerAndAccount() {
        Long first = createCustomer(1);
        Long second = createCustomer(2);
        CustomerForm unchanged = service.getForm(first);
        assertThat(unchanged.getPassword()).isNull();
        assertThatCode(() -> service.update(first, unchanged)).doesNotThrowAnyException();
        for (String field : new String[]{"email", "mobile", "identityCard"}) {
            CustomerForm duplicate = service.getForm(first);
            CustomerForm other = service.getForm(second);
            switch (field) {
                case "email" -> duplicate.setEmail(other.getEmail());
                case "mobile" -> duplicate.setMobile(other.getMobile());
                default -> duplicate.setIdentityCard(other.getIdentityCard());
            }
            assertThatThrownBy(() -> service.update(first, duplicate)).isInstanceOf(BusinessException.class);
        }
        accounts.saveAndFlush(new Account("admin-only@example.test", encoder.encode("Example@123"),
                Role.ADMIN, AccountStatus.ACTIVE));
        CustomerForm conflict = form(3);
        conflict.setEmail("admin-only@example.test");
        assertThatThrownBy(() -> service.create(conflict)).hasMessageContaining("Email");
        CustomerForm update = service.getForm(first);
        update.setEmail("admin-only@example.test");
        assertThatThrownBy(() -> service.update(first, update)).hasMessageContaining("Email");
        assertThat(accounts.findByUsername("customer1@example.test")).isPresent();
    }

    @Test
    void editKeepsBlankPasswordAndOnlyUsesIdFromPathThenChangesLoginEmailAndPassword() throws Exception {
        Long id = createCustomer(1);
        Long other = createCustomer(2);
        String oldHash = accounts.findByUsername("customer1@example.test").orElseThrow().getPassword();
        CustomerForm edit = form(1);
        edit.setPassword("");
        edit.setFullName("Edited");
        mvc.perform(postForm("/admin/customers/" + id + "/edit", edit).param("customerId", other.toString()))
                .andExpect(redirectedUrl("/admin/customers"));
        assertThat(accounts.findByUsername("customer1@example.test").orElseThrow().getPassword()).isEqualTo(oldHash);
        assertThat(service.getForm(other).getFullName()).isEqualTo("Customer 2");
        assertThat(auth.login("customer1@example.test", "Example@123").getDisplayName()).isEqualTo("Edited");
        edit.setEmail("new@example.test");
        edit.setPassword("Changed@123");
        mvc.perform(postForm("/admin/customers/" + id + "/edit", edit)).andExpect(redirectedUrl("/admin/customers"));
        assertThat(accounts.findByUsername("customer1@example.test")).isEmpty();
        assertThat(auth.login("new@example.test", "Changed@123").getCustomerId()).isEqualTo(id);
        assertThatThrownBy(() -> auth.login("new@example.test", "Example@123")).isInstanceOf(BusinessException.class);
    }

    @Test
    void missingCustomerRenders404AndWritesRequireCsrfAndAdmin() throws Exception {
        mvc.perform(get("/admin/customers/-1/edit").session(admin())).andExpect(status().isNotFound());
        mvc.perform(post("/admin/customers/new").session(admin())).andExpect(status().isForbidden());
        mvc.perform(get("/admin/customers/new")).andExpect(redirectedUrl("/login"));
        MockHttpSession customer = new MockHttpSession();
        customer.setAttribute(SessionUtils.CURRENT_USER, new SessionUser(1L, 2L, "An", Role.CUSTOMER));
        mvc.perform(postForm("/admin/customers/new", form(1)).session(customer)).andExpect(status().isForbidden());
        assertThat(accounts.count()).isZero();
    }

    @Test
    void serviceRejectsUnderageWeakOrOversizedPasswordsAndTooLongEmail() {
        CustomerForm underage = form(1);
        underage.setBirthday(LocalDate.now().minusYears(13).plusDays(1));
        assertThatThrownBy(() -> service.create(underage)).hasMessageContaining("13");
        CustomerForm weak = form(1);
        weak.setPassword("weak");
        assertThatThrownBy(() -> service.create(weak)).isInstanceOf(BusinessException.class);
        CustomerForm oversized = form(1);
        oversized.setPassword("Aa1" + "x".repeat(70));
        assertThatThrownBy(() -> service.create(oversized)).hasMessageContaining("quá dài");
        CustomerForm email = form(1);
        email.setEmail("x".repeat(90) + "@example.test");
        assertThatThrownBy(() -> service.create(email)).isInstanceOf(BusinessException.class);
        assertThat(accounts.count()).isZero();
    }

    @Test
    void customerSaveFailureRollsBackAccountOutsideAnyTestTransaction() {
        doThrow(new DataIntegrityViolationException("simulated unique race")).when(customers).saveAndFlush(any());
        try {
            assertThatThrownBy(() -> service.create(form(1))).isInstanceOf(BusinessException.class);
            assertThat(accounts.count()).isZero();
            assertThat(customers.count()).isZero();
        } finally {
            reset(customers);
        }
    }
}
