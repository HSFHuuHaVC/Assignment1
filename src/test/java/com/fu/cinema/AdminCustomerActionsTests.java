package com.fu.cinema;

import com.fu.cinema.dto.CustomerForm;
import com.fu.cinema.dto.SessionUser;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:customer_actions;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class AdminCustomerActionsTests {
    @Autowired MockMvc mvc;
    @Autowired CustomerService service;
    @Autowired AuthService auth;
    @Autowired AccountRepository accounts;
    @Autowired CustomerRepository customers;
    private Long customerId;

    @BeforeEach
    void createCustomer() {
        CustomerForm form = new CustomerForm();
        form.setFullName("Nguyen Van An");
        form.setEmail("an@example.test");
        form.setMobile("0901234567");
        form.setIdentityCard("012345678901");
        form.setBirthday(LocalDate.of(2000, 1, 1));
        form.setPassword("Example@123");
        service.create(form);
        customerId = auth.login("an@example.test", "Example@123").getCustomerId();
    }

    private MockHttpSession session(Role role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionUtils.CURRENT_USER, new SessionUser(-1L,
                role == Role.CUSTOMER ? customerId : null, "Tester", role));
        return session;
    }

    @Test
    void lockingBlocksLoginAndUnlockingRestoresItWithoutChangingPassword() throws Exception {
        String hash = accounts.findByUsername("an@example.test").orElseThrow().getPassword();
        for (AccountStatus expected : new AccountStatus[]{AccountStatus.LOCKED, AccountStatus.ACTIVE}) {
            mvc.perform(post("/admin/customers/" + customerId + "/lock").session(session(Role.ADMIN)).with(csrf()))
                    .andExpect(redirectedUrl("/admin/customers"))
                    .andExpect(flash().attribute("success", "Đã cập nhật trạng thái tài khoản"));
            assertThat(service.getForm(customerId).getStatus()).isEqualTo(expected);
            if (expected == AccountStatus.LOCKED) {
                assertThatThrownBy(() -> auth.login("an@example.test", "Example@123")).isInstanceOf(BusinessException.class);
            } else {
                assertThat(auth.login("an@example.test", "Example@123").getCustomerId()).isEqualTo(customerId);
            }
        }
        assertThat(accounts.findByUsername("an@example.test").orElseThrow().getPassword()).isEqualTo(hash);
    }

    @Test
    void lockRequiresAdminPostAndCsrf() throws Exception {
        String path = "/admin/customers/" + customerId + "/lock";
        mvc.perform(get(path).session(session(Role.ADMIN))).andExpect(status().isMethodNotAllowed());
        mvc.perform(post(path).session(session(Role.ADMIN))).andExpect(status().isForbidden());
        mvc.perform(post(path).with(csrf())).andExpect(redirectedUrl("/login"));
        mvc.perform(post(path).session(session(Role.CUSTOMER)).with(csrf())).andExpect(status().isForbidden());
        assertThat(service.getForm(customerId).getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void deleteWithoutBookingsRemovesBothRowsAndRedirectsWithFlash() throws Exception {
        Long accountId = auth.login("an@example.test", "Example@123").getAccountId();
        mvc.perform(post("/admin/customers/" + customerId + "/delete").session(session(Role.ADMIN)).with(csrf()))
                .andExpect(redirectedUrl("/admin/customers")).andExpect(flash().attribute("success", "Đã xoá khách hàng"));
        assertThat(customers.findById(customerId)).isEmpty();
        assertThat(accounts.findById(accountId)).isEmpty();
    }

    @Test
    void deletionRequiresAdminPostCsrfAndExistingCustomer() throws Exception {
        String path = "/admin/customers/" + customerId + "/delete";
        mvc.perform(get(path).session(session(Role.ADMIN))).andExpect(status().isMethodNotAllowed());
        mvc.perform(post(path).session(session(Role.ADMIN))).andExpect(status().isForbidden());
        mvc.perform(post(path).with(csrf())).andExpect(redirectedUrl("/login"));
        mvc.perform(post(path).session(session(Role.CUSTOMER)).with(csrf())).andExpect(status().isForbidden());
        assertThat(customers.findById(customerId)).isPresent();
        mvc.perform(post("/admin/customers/-1/delete").session(session(Role.ADMIN)).with(csrf()))
                .andExpect(status().isNotFound());
    }
}
