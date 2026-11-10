package com.fu.cinema.desktop;

import com.fu.cinema.FuCinemaTicketSystemApplication;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.enums.Role;
import com.fu.cinema.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

class DesktopBootstrapTests {
    @Test
    void nonWebSpringContextStartsAndClosesWithoutJavaFxToolkit() {
        try (var context = new SpringApplicationBuilder(FuCinemaTicketSystemApplication.class)
                .web(WebApplicationType.NONE).profiles("test", "desktop")
                .run("--spring.profiles.active=test", "--spring.datasource.url=jdbc:h2:mem:desktopBootstrap;MODE=MSSQLServer")) {
            assertFalse(context instanceof WebApplicationContext);
            assertEquals("none", context.getEnvironment().getProperty("spring.main.web-application-type"));
            assertNotNull(context.getBean(DesktopSession.class));
        }
    }

    @Test
    void sessionDefaultsToDeniedAndLogoutRevokesAdmin() {
        var session = new DesktopSession();
        assertThrows(BusinessException.class, session::requireAdmin);
        session.login(new SessionUser(1L, null, "Admin", Role.ADMIN));
        assertDoesNotThrow(session::requireAdmin);
        assertEquals("Admin", session.displayNameProperty().get());
        session.logout();
        assertNull(session.getUser());
        assertEquals("", session.displayNameProperty().get());
        assertThrows(BusinessException.class, session::requireAdmin);
    }

    @Test
    void sessionRejectsCustomerWithoutAuthenticating() {
        var session = new DesktopSession();
        assertThrows(BusinessException.class,
                () -> session.login(new SessionUser(2L, 1L, "Customer", Role.CUSTOMER)));
        assertNull(session.getUser());
    }
}
