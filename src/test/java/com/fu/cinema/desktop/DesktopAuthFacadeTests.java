package com.fu.cinema.desktop;

import com.fu.cinema.desktop.facade.AuthFacade;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.enums.AccountStatus;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.repository.AccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.datasource.url=jdbc:h2:mem:desktopAuth;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@ActiveProfiles({"test", "desktop"})
class DesktopAuthFacadeTests {
    @Autowired AuthFacade facade;
    @Autowired DesktopSession session;
    @Autowired AccountRepository accounts;

    @AfterEach void clearSession() { session.logout(); }

    @Test void acceptsAdminAndSessionLogoutRevokesAccess() {
        var user = facade.loginAdmin("admin@fucinema.vn", "Admin@123");
        assertTrue(user.isAdmin());
        session.login(user);
        assertDoesNotThrow(session::requireAdmin);
        session.logout();
        assertThrows(BusinessException.class, session::requireAdmin);
    }
    @Test void rejectsCustomerEvenWhenPasswordIsCorrect() {
        var error = assertThrows(BusinessException.class,
                () -> facade.loginAdmin("an.nv@gmail.com", "Customer@123"));
        assertTrue(error.getMessage().contains("ADMIN"));
        assertNull(session.getUser());
    }
    @Test void rejectsWrongPassword() {
        assertThrows(BusinessException.class, () -> facade.loginAdmin("admin@fucinema.vn", "wrong"));
        assertNull(session.getUser());
    }
    @Test @Transactional void rejectsLockedAdminThroughExistingAuthService() {
        var account = accounts.findByUsername("admin@fucinema.vn").orElseThrow();
        account.setStatus(AccountStatus.LOCKED);
        accounts.saveAndFlush(account);
        var error = assertThrows(BusinessException.class,
                () -> facade.loginAdmin("admin@fucinema.vn", "Admin@123"));
        assertTrue(error.getMessage().contains("khóa"));
    }
}
