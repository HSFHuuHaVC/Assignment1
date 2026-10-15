package com.fu.cinema;

import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.enums.Role;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.exception.ResourceNotFoundException;
import com.fu.cinema.util.SessionUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "issue5-fixture"})
@AutoConfigureMockMvc(addFilters = false)
@Import({ErrorHandlingTests.FixtureController.class, ErrorHandlingTests.TestSecurity.class})
class ErrorHandlingTests {
    @Autowired MockMvc mvc;
    @LocalServerPort int port;

    @Test
    void missingRecordReturns404WithSharedGuestLayout() throws Exception {
        String html = mvc.perform(get("/__issue5/movies/9999"))
                .andExpect(status().isNotFound()).andExpect(view().name("error/404"))
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("404", "không tồn tại", "Đăng nhập", "Đăng ký", "FU Cinema Ticket System")
                .doesNotContain("ResourceNotFoundException", "id = 9999", "TEST_INTERNAL_SECRET");
    }

    @Test
    void invalidIdAndMissingStaticResourceReturn404() throws Exception {
        mvc.perform(get("/__issue5/movies/abc"))
                .andExpect(status().isNotFound()).andExpect(view().name("error/404"));
        mvc.perform(get("/not-an-existing-resource.css"))
                .andExpect(status().isNotFound()).andExpect(view().name("error/404"));
    }

    @Test
    void forbiddenOperationAndExplicitHttpStatusReturn403() throws Exception {
        mvc.perform(get("/__issue5/forbidden"))
                .andExpect(status().isForbidden()).andExpect(view().name("error/403"));
        mvc.perform(get("/__issue5/status-forbidden"))
                .andExpect(status().isForbidden()).andExpect(view().name("error/403"));
    }

    @Test
    void unexpectedErrorReturns500WithoutInternalDetails() throws Exception {
        String html = mvc.perform(get("/__issue5/unexpected"))
                .andExpect(status().isInternalServerError()).andExpect(view().name("error/500"))
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("500", "Vui lòng thử lại sau", "FU Cinema Ticket System")
                .doesNotContain("TEST_INTERNAL_SECRET", "IllegalStateException", "java.lang", "ErrorHandlingTests");
    }

    @Test
    void businessErrorRedirectsBackWithEscapedFlashMessage() throws Exception {
        MvcResult result = mvc.perform(post("/__issue5/business")
                        .header("Referer", "http://localhost/__issue5/result?kw=action"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/__issue5/result?kw=action"))
                .andExpect(flash().attribute("error", "Không thể xóa thể loại <script>alert(1)</script>"))
                .andReturn();
        String html = mvc.perform(get("/__issue5/result").flashAttrs(result.getFlashMap()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("alert-danger", "&lt;script&gt;alert(1)&lt;/script&gt;")
                .doesNotContain("<script>alert(1)</script>");
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://other.example/path", "//other.example/path", "javascript:alert(1)",
            "http://localhost:81/path", "http://localhost@other.example/path", "not a URI"})
    void businessRedirectCannotLeaveCurrentOrigin(String referer) throws Exception {
        mvc.perform(post("/__issue5/business").header("Referer", referer))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/"));
    }

    @Test
    void missingRefererFallsBackToHomeAndRelativeRefererKeepsFilters() throws Exception {
        mvc.perform(post("/__issue5/business"))
                .andExpect(redirectedUrl("/"));
        mvc.perform(post("/__issue5/business").header("Referer", "/admin/genres?kw=action"))
                .andExpect(redirectedUrl("/admin/genres?kw=action"));
    }

    @Test
    void frameworkMethodNotAllowedKeeps405InsteadOfBecoming500() throws Exception {
        mvc.perform(get("/__issue5/business"))
                .andExpect(status().isMethodNotAllowed()).andExpect(view().name("error"));
    }

    @Test
    void errorLayoutUsesSessionRole() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionUtils.CURRENT_USER, new SessionUser(1L, null, "Admin", Role.ADMIN));
        String html = mvc.perform(get("/__issue5/movies/9999").session(session))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("href=\"/admin/movies\"", "Xin chào, Admin", "Đăng xuất")
                .doesNotContain("href=\"/register\"", "href=\"/customer/bookings\"");
    }

    @ParameterizedTest
    @ValueSource(ints = {403, 404, 500, 422})
    void servletSendErrorUsesCustomPagesWithoutExposingErrorAttributes(int code) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(
                "http://127.0.0.1:" + port + "/__issue5/send-error/" + code + "?trace=true"))
                .header("Accept", "text/html").timeout(Duration.ofSeconds(10)).GET().build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(code);
        assertThat(response.body()).contains(String.valueOf(code), "FU Cinema Ticket System")
                .doesNotContain("TEST_INTERNAL_SECRET", "Whitelabel Error Page", "java.lang", "ErrorHandlingTests");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestSecurity {
        @Bean
        SecurityFilterChain errorFixtureSecurity(HttpSecurity http) throws Exception {
            return http.csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll()).build();
        }
    }

    @Controller
    @Profile("issue5-fixture")
    static class FixtureController {
        @GetMapping("/__issue5/movies/{id}")
        String missing(@PathVariable Long id) {
            throw new ResourceNotFoundException("Phim", id);
        }

        @GetMapping("/__issue5/forbidden")
        String forbidden() {
            throw new AccessDeniedException("TEST_INTERNAL_SECRET");
        }

        @GetMapping("/__issue5/status-forbidden")
        String forbiddenStatus() {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "TEST_INTERNAL_SECRET");
        }

        @GetMapping("/__issue5/unexpected")
        String unexpected() {
            throw new IllegalStateException("TEST_INTERNAL_SECRET");
        }

        @PostMapping("/__issue5/business")
        String business() {
            throw new BusinessException("Không thể xóa thể loại <script>alert(1)</script>");
        }

        @GetMapping("/__issue5/result")
        String result() {
            return "error-fixture-result";
        }

        @GetMapping("/__issue5/send-error/{code}")
        void servletError(@PathVariable int code, HttpServletResponse response) throws IOException {
            response.sendError(code, "TEST_INTERNAL_SECRET");
        }

        @GetMapping("/__issue5/session")
        String session(@RequestParam(defaultValue = "guest") String role, HttpServletRequest request) {
            if (role.equals("guest")) {
                if (request.getSession(false) != null) {
                    request.getSession(false).removeAttribute(SessionUtils.CURRENT_USER);
                }
            } else {
                Role selected = role.equals("admin") ? Role.ADMIN : Role.CUSTOMER;
                request.getSession().setAttribute(SessionUtils.CURRENT_USER,
                        new SessionUser(1L, selected == Role.CUSTOMER ? 2L : null, role, selected));
            }
            return "redirect:/__issue5/movies/9999";
        }
    }
}
