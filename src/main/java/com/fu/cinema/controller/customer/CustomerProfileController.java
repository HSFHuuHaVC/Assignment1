package com.fu.cinema.controller.customer;

import com.fu.cinema.dto.ProfileForm;
import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.service.CustomerService;
import com.fu.cinema.util.SessionUtils;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerProfileController {
    private final CustomerService customerService;

    @InitBinder("form")
    public void configureForm(WebDataBinder binder) {
        binder.setAllowedFields("fullName", "email", "mobile", "birthday", "identityCard");
        for (String field : new String[]{"fullName", "email", "mobile", "identityCard"}) {
            binder.registerCustomEditor(String.class, field, new StringTrimmerEditor(true));
        }
    }

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        model.addAttribute("form", customerService.getProfile(requireCustomer(session).getCustomerId()));
        return "customer/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute("form") ProfileForm form, BindingResult binding,
                                HttpSession session, Model model, RedirectAttributes flash) {
        SessionUser user = requireCustomer(session);
        form.setUsername(customerService.getProfile(user.getCustomerId()).getUsername());
        if (binding.hasErrors()) return "customer/profile";
        try {
            customerService.updateProfile(user.getCustomerId(), form);
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            return "customer/profile";
        }
        session.setAttribute(SessionUtils.CURRENT_USER,
                new SessionUser(user.getAccountId(), user.getCustomerId(), form.getFullName(), user.getRole()));
        flash.addFlashAttribute("success", "Đã cập nhật hồ sơ");
        return "redirect:/customer/profile";
    }

    private SessionUser requireCustomer(HttpSession session) {
        SessionUser user = SessionUtils.getCurrentUser(session);
        if (user == null || !user.isCustomer() || user.getCustomerId() == null || user.getAccountId() == null) {
            throw new AccessDeniedException("Vui lòng đăng nhập bằng tài khoản khách hàng");
        }
        return user;
    }
}
