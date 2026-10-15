package com.fu.cinema;

import com.fu.cinema.dto.RegisterForm;
import com.fu.cinema.repository.AccountRepository;
import com.fu.cinema.repository.CustomerRepository;
import com.fu.cinema.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import java.net.*;
import java.net.http.*;
import java.time.LocalDate;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:auth_forbidden;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@ActiveProfiles({"test", "auth-fixture"})
class AuthForbiddenPageTests {
    @LocalServerPort int port;
    @Autowired AuthService auth;
    @Autowired CustomerRepository customers;
    @Autowired AccountRepository accounts;

    @Test
    void realServletErrorDispatchRenders403AndPreservesCustomerMenu() throws Exception {
        RegisterForm form = new RegisterForm();
        form.setFullName("Forbidden Customer");
        form.setEmail("forbidden@example.test");
        form.setMobile("0999999999");
        form.setIdentityCard("999999999999");
        form.setBirthday(LocalDate.now().minusYears(20));
        form.setPassword("Example@123");
        form.setConfirmPassword("Example@123");
        var user = auth.register(form);
        try {
            var cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
            var client = HttpClient.newBuilder().cookieHandler(cookies).build();
            String base = "http://127.0.0.1:" + port;
            var login = client.send(HttpRequest.newBuilder(URI.create(base + "/login")).build(),
                    HttpResponse.BodyHandlers.ofString());
            var matcher = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(login.body());
            assertThat(matcher.find()).isTrue();
            String body = "username=forbidden%40example.test&password=Example%40123&_csrf=" +
                    URLEncoder.encode(matcher.group(1), java.nio.charset.StandardCharsets.UTF_8);
            var signedIn = client.send(HttpRequest.newBuilder(URI.create(base + "/login"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
            assertThat(signedIn.statusCode()).isEqualTo(302);
            var denied = client.send(HttpRequest.newBuilder(URI.create(base + "/admin"))
                    .header("Accept", "text/html").build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(denied.statusCode()).isEqualTo(403);
            assertThat(denied.body()).contains("Bạn không có quyền truy cập", "Forbidden Customer",
                    "Vé của tôi", "Đăng xuất", "chọn chức năng phù hợp").doesNotContain("Whitelabel", "Exception", "Dashboard");
        } finally {
            customers.deleteById(user.getCustomerId());
            accounts.deleteById(user.getAccountId());
        }
    }
}
