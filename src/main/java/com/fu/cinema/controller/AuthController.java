package com.fu.cinema.controller;

import com.fu.cinema.dto.LoginForm;
import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.service.AuthService;
import com.fu.cinema.util.SessionUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @InitBinder
    public void trimIdentityFields(WebDataBinder binder) {
        for (String field : new String[]{"username", "email", "fullName", "mobile", "identityCard"}) {
            binder.registerCustomEditor(String.class, field, new StringTrimmerEditor(true));
        }
    }

    @GetMapping("/login")
    public String loginPage(HttpServletRequest request, Model model) {
        SessionUser user = SessionUtils.getCurrentUser(request.getSession(false));
        if (user != null) {
            return destination(user);
        }
        model.addAttribute("form", new LoginForm());
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute("form") LoginForm form, BindingResult binding,
                        HttpServletRequest request, Model model) {
        if (binding.hasErrors()) {
            return "auth/login";
        }
        try {
            SessionUser user = authService.login(form.getUsername(), form.getPassword());
            startSession(request, user);
            return destination(user);
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            return "auth/login";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request, RedirectAttributes flash) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        flash.addFlashAttribute("success", "Đã đăng xuất");
        return "redirect:/login";
    }

    private String destination(SessionUser user) {
        return user.isAdmin() ? "redirect:/admin" : "redirect:/";
    }

    private void startSession(HttpServletRequest request, SessionUser user) {
        HttpSession old = request.getSession(false);
        if (old != null) {
            old.invalidate();
        }
        request.getSession(true).setAttribute(SessionUtils.CURRENT_USER, user);
    }
}
