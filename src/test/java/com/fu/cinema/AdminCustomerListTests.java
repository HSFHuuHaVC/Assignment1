package com.fu.cinema;

import com.fu.cinema.dto.CustomerForm;
import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.enums.Role;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:customer_list;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class AdminCustomerListTests {
    @Autowired MockMvc mvc;
    @Autowired CustomerService service;

    @BeforeEach
    void seed() {
        for (int i = 0; i < 12; i++) {
            CustomerForm form = new CustomerForm();
            form.setFullName(String.format("Nguyen %02d", i));
            form.setEmail("customer" + i + "@gmail.test");
            form.setMobile(String.format("0912%06d", i));
            form.setIdentityCard(String.format("%012d", i));
            form.setBirthday(LocalDate.of(2000, 1, 2));
            form.setPassword("Example@123");
            service.create(form);
        }
    }

    private MockHttpSession admin() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionUtils.CURRENT_USER, new SessionUser(1L, null, "Admin", Role.ADMIN));
        return session;
    }

    @Test
    void searchRetainsKeywordAcrossPagesAndRendersFetchedAccountStatus() throws Exception {
        String html = mvc.perform(get("/admin/customers").session(admin()).queryParam("kw", "GMAIL").queryParam("page", "0"))
                .andExpect(status().isOk()).andExpect(view().name("admin/customers/list"))
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Nguyen 00", "Nguyen 09", "ACTIVE", "02/01/2000", "kw=GMAIL&amp;page=1")
                .doesNotContain("Nguyen 10");
        String second = mvc.perform(get("/admin/customers").session(admin()).queryParam("kw", "GMAIL").queryParam("page", "1"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(second).contains("Nguyen 10", "Nguyen 11", "kw=GMAIL&amp;page=0").doesNotContain("Nguyen 00");
    }

    @Test
    void searchesNameMobileAndEmptyResult() throws Exception {
        for (String kw : new String[]{"nguyen", "0912"}) {
            mvc.perform(get("/admin/customers").session(admin()).queryParam("kw", kw))
                    .andExpect(status().isOk()).andExpect(model().attributeExists("customers"));
        }
        String html = mvc.perform(get("/admin/customers").session(admin()).queryParam("kw", "<script>"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Không có dữ liệu", "&lt;script&gt;").doesNotContain("<script>");
    }

    @Test
    void onlyAdminCanReadCustomers() throws Exception {
        mvc.perform(get("/admin/customers")).andExpect(redirectedUrl("/login"));
        MockHttpSession customer = new MockHttpSession();
        customer.setAttribute(SessionUtils.CURRENT_USER, new SessionUser(1L, 2L, "An", Role.CUSTOMER));
        mvc.perform(get("/admin/customers").session(customer)).andExpect(status().isForbidden());
    }
}
