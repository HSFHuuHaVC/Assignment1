package com.fu.cinema.util;

import com.fu.cinema.dto.SessionUser;
import jakarta.servlet.http.HttpSession;

public final class SessionUtils {

    public static final String CURRENT_USER = "currentUser";

    private SessionUtils() {
    }

    public static SessionUser getCurrentUser(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(CURRENT_USER);
        return value instanceof SessionUser user ? user : null;
    }

    /** Luôn lấy customerId từ session, KHÔNG lấy từ form/URL. */
    public static Long getCurrentCustomerId(HttpSession session) {
        SessionUser user = getCurrentUser(session);
        return user != null && user.isCustomer() ? user.getCustomerId() : null;
    }
}
