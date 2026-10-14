package com.fu.cinema;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Import(LayoutFragmentTests.LayoutTestController.class)
class LayoutFragmentTests {

    @Autowired
    private MockMvc mvc;

    @Test
    void guestSeesPublicMenuAndSharedLayout() throws Exception {
        String html = render("guest", 0);
        assertThat(html).contains("<title>Trang chủ | FU Cinema</title>", "Đăng nhập", "Đăng ký",
                "/css/app.css", "FU Cinema Ticket System");
        assertThat(menu(html)).doesNotContain("href=\"/admin", "href=\"/customer", "Đăng xuất");
    }

    @Test
    void adminSeesOnlyAdminMenuAndPostLogout() throws Exception {
        String html = render("admin", 0);
        assertThat(html).contains("href=\"/admin/movies\"", "href=\"/admin/reports\"",
                "Xin chào, Admin", "action=\"/logout\"", "method=\"post\"");
        assertThat(menu(html)).doesNotContain("href=\"/customer", "Đăng nhập", "Đăng ký");
    }

    @Test
    void customerSeesOnlyCustomerMenu() throws Exception {
        String html = render("customer", 0);
        assertThat(html).contains("href=\"/customer/bookings\"", "href=\"/customer/profile\"");
        assertThat(menu(html)).doesNotContain("href=\"/admin", "Đăng nhập", "Đăng ký");
    }

    @Test
    void flashMessagesEscapeUserInput() throws Exception {
        String html = mvc.perform(get("/__layout-test")
                        .param("success", "Đã lưu phim")
                        .param("error", "<script>alert(1)</script>"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Đã lưu phim", "&lt;script&gt;alert(1)&lt;/script&gt;");
        assertThat(html).doesNotContain("<script>alert(1)</script>");
    }

    @Test
    void paginationPreservesFiltersAndDisablesBoundaryNavigation() throws Exception {
        assertThat(render("guest", 0)).contains("genreId=7&amp;page=1", "aria-disabled=\"true\"")
                .doesNotContain("page=-1");
        assertThat(render("guest", 1)).contains("genreId=7&amp;page=0", "genreId=7&amp;page=2",
                "aria-current=\"page\"");
        assertThat(render("guest", 2)).doesNotContain("page=3");
    }

    @Test
    void emptyResultsDoNotRenderPagination() throws Exception {
        String html = mvc.perform(get("/__layout-test").param("total", "0"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).doesNotContain("aria-label=\"Phân trang\"");
    }

    private String menu(String html) {
        return html.substring(0, html.indexOf("<main"));
    }

    private String render(String role, int page) throws Exception {
        return mvc.perform(get("/__layout-test").param("role", role).param("page", String.valueOf(page)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    // These fixtures are in src/test and are never packaged with the application.
    @Controller
    static class LayoutTestController {
        @GetMapping("/__layout-test")
        String layout(@RequestParam(defaultValue = "guest") String role,
                      @RequestParam(defaultValue = "0") int page,
                      @RequestParam(defaultValue = "21") long total,
                      @RequestParam(required = false) String success,
                      @RequestParam(required = false) String error, Model model) {
            if (!role.equals("guest")) {
                model.addAttribute("currentUser", new PreviewUser(role));
            }
            model.addAttribute("success", success);
            model.addAttribute("error", error);
            model.addAttribute("page", new PageImpl<>(total == 0 ? List.of() : List.of("Movie"),
                    PageRequest.of(page, 10), total));
            model.addAttribute("url", "/admin/movies?kw=cinema&genreId=7");
            return "layout-test";
        }
    }

    public static class PreviewUser {
        private final String role;

        PreviewUser(String role) {
            this.role = role;
        }

        public boolean isAdmin() {
            return role.equals("admin");
        }

        public boolean isCustomer() {
            return role.equals("customer");
        }

        public String getDisplayName() {
            return isAdmin() ? "Admin" : "Customer";
        }
    }
}
