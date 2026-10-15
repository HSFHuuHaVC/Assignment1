package com.fu.cinema.controller.admin;

import com.fu.cinema.service.CustomerService;
import com.fu.cinema.dto.CustomerForm;
import com.fu.cinema.enums.AccountStatus;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.util.PageUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/customers")
@RequiredArgsConstructor
public class AdminCustomerController {
    private static final String FORM_VIEW = "admin/customers/form";
    private final CustomerService customerService;

    @InitBinder("form")
    public void configureForm(WebDataBinder binder) {
        binder.setAllowedFields("fullName", "email", "mobile", "birthday", "identityCard", "password", "status");
        for (String field : new String[]{"fullName", "email", "mobile", "identityCard"}) {
            binder.registerCustomEditor(String.class, field, new StringTrimmerEditor(true));
        }
    }

    @ModelAttribute("statuses")
    public AccountStatus[] statuses() {
        return AccountStatus.values();
    }

    @GetMapping
    public String list(@RequestParam(required = false) String kw,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("customers", customerService.search(kw, page, PageUtils.DEFAULT_SIZE));
        model.addAttribute("kw", kw);
        model.addAttribute("pageUrl", PageUtils.currentUrlWithoutPage());
        return "admin/customers/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new CustomerForm());
        return FORM_VIEW;
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("form") CustomerForm form, BindingResult binding,
                         Model model, RedirectAttributes flash) {
        form.setCustomerId(null);
        if ((form.getPassword() == null || form.getPassword().isBlank())
                && !binding.hasFieldErrors("password")) {
            binding.rejectValue("password", "required", "Mật khẩu không được để trống khi tạo khách hàng");
        }
        if (binding.hasErrors()) return FORM_VIEW;
        try {
            customerService.create(form);
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            return FORM_VIEW;
        }
        flash.addFlashAttribute("success", "Đã thêm khách hàng " + form.getFullName());
        return "redirect:/admin/customers";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        boolean deleted = customerService.delete(id);
        flash.addFlashAttribute("success", deleted
                ? "Đã xoá khách hàng"
                : "Khách hàng đã có đơn đặt vé nên chỉ khoá tài khoản");
        return "redirect:/admin/customers";
    }

    @PostMapping("/{id}/lock")
    public String toggleLock(@PathVariable Long id, RedirectAttributes flash) {
        customerService.toggleLock(id);
        flash.addFlashAttribute("success", "Đã cập nhật trạng thái tài khoản");
        return "redirect:/admin/customers";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", customerService.getForm(id));
        return FORM_VIEW;
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") CustomerForm form,
                         BindingResult binding, Model model, RedirectAttributes flash) {
        form.setCustomerId(id);
        if (binding.hasErrors()) return FORM_VIEW;
        try {
            customerService.update(id, form);
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            return FORM_VIEW;
        }
        flash.addFlashAttribute("success", "Đã cập nhật khách hàng " + form.getFullName());
        return "redirect:/admin/customers";
    }
}
