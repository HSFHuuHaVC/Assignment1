package com.fu.cinema;

import com.fu.cinema.controller.GlobalModelAttributes;
import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.enums.Role;
import com.fu.cinema.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class SharedHelperTests {
    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void blankToNullHandlesMissingWhitespaceAndTrimsText() {
        assertThat(ValidationUtils.blankToNull(null)).isNull();
        assertThat(ValidationUtils.blankToNull(" \t\n")).isNull();
        assertThat(ValidationUtils.blankToNull("  Chú mèo  ")).isEqualTo("Chú mèo");
    }

    @Test
    void ageChangesAtBirthdayRatherThanStartOfYear() {
        LocalDate today = LocalDate.now();
        assertThat(ValidationUtils.age(today.minusYears(18))).isEqualTo(18);
        assertThat(ValidationUtils.age(today.minusYears(18).plusDays(1))).isEqualTo(17);
        assertThat(ValidationUtils.age(today.minusYears(18).minusDays(1))).isEqualTo(18);
    }

    @Test
    void datesAreFormattedForViewsAndNullHasNoPlaceholder() {
        assertThat(DateUtils.format(null)).isEmpty();
        assertThat(DateUtils.format(LocalDateTime.of(2026, 10, 15, 9, 5)))
                .isEqualTo("15/10/2026 09:05");
    }

    @Test
    void absentOrUnrelatedSessionAttributeDoesNotBecomeCurrentUser() {
        assertThat(SessionUtils.getCurrentUser(null)).isNull();
        assertThat(SessionUtils.getCurrentCustomerId(null)).isNull();
        MockHttpSession session = new MockHttpSession();
        assertThat(SessionUtils.getCurrentUser(session)).isNull();
        session.setAttribute(SessionUtils.CURRENT_USER, "not a session user");
        assertThat(SessionUtils.getCurrentUser(session)).isNull();
    }

    @Test
    void customerIdentityComesOnlyFromCustomerSession() {
        MockHttpSession session = new MockHttpSession();
        SessionUser customer = new SessionUser(1L, 9L, "An", Role.CUSTOMER);
        session.setAttribute(SessionUtils.CURRENT_USER, customer);
        assertThat(SessionUtils.getCurrentUser(session)).isSameAs(customer);
        assertThat(SessionUtils.getCurrentCustomerId(session)).isEqualTo(9L);
        session.setAttribute(SessionUtils.CURRENT_USER, new SessionUser(2L, 99L, "Admin", Role.ADMIN));
        assertThat(SessionUtils.getCurrentCustomerId(session)).isNull();
    }

    @Test
    void globalViewAttributesDoNotCreateSessionForGuest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        GlobalModelAttributes attributes = new GlobalModelAttributes();
        assertThat(attributes.currentUser(request)).isNull();
        assertThat(request.getSession(false)).isNull();
        SessionUser user = new SessionUser(1L, 9L, "An", Role.CUSTOMER);
        request.getSession().setAttribute(SessionUtils.CURRENT_USER, user);
        assertThat(attributes.currentUser(request)).isEqualTo(user);
    }

    @Test
    void pagingRemovesAllPageParametersButKeepsFiltersAndEncoding() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/movies");
        request.setQueryString("page=2&kw=Ch%C3%BA+m%C3%A8o&genreId=1&genreId=2&page=3&sort=title%2Casc");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        assertThat(PageUtils.currentUrlWithoutPage()).isEqualTo(
                "http://localhost/admin/movies?kw=Ch%C3%BA+m%C3%A8o&genreId=1&genreId=2&sort=title%2Casc");
    }

    @Test
    void pagingWithoutFiltersDoesNotLeaveDanglingQuestionMark() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/movies");
        request.setQueryString("page=2");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        assertThat(PageUtils.currentUrlWithoutPage()).isEqualTo("http://localhost/movies");
    }
}
