package com.fu.cinema.controller;

import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.util.SessionUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

// Đưa người dùng hiện tại vào Model của mọi view (menu theo vai trò)
@ControllerAdvice
public class GlobalModelAttributes {

    @ModelAttribute("currentUser")
    public SessionUser currentUser(HttpServletRequest request) {
        return SessionUtils.getCurrentUser(request.getSession(false));   // false: không tạo session mới
    }
}
