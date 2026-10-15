package com.fu.cinema.config;

import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.util.SessionUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.UriUtils;
import java.nio.charset.StandardCharsets;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String path = UriUtils.decode(request.getRequestURI().substring(request.getContextPath().length()),
                StandardCharsets.UTF_8);
        boolean adminPath = path.equals("/admin") || path.startsWith("/admin/");
        boolean customerPath = path.equals("/customer") || path.startsWith("/customer/");
        if (!adminPath && !customerPath) {
            return true;
        }
        SessionUser user = SessionUtils.getCurrentUser(request.getSession(false));
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }
        if ((adminPath && !user.isAdmin()) || (customerPath && !user.isCustomer())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        return true;
    }
}
