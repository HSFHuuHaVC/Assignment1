package com.fu.cinema.controller.admin;

import com.fu.cinema.dto.GenreForm;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.service.GenreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/genres")
@RequiredArgsConstructor
public class AdminGenreController {

    private static final String FORM_VIEW = "admin/genres/form";

    private final GenreService genreService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("genres", genreService.findAll());
        return "admin/genres/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new GenreForm());
        return FORM_VIEW;
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("form") GenreForm form, BindingResult br,
                         Model model, RedirectAttributes ra) {
        if (br.hasErrors()) {
            return FORM_VIEW;
        }
        try {
            genreService.create(form);
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return FORM_VIEW;
        }
        ra.addFlashAttribute("success", "Đã thêm thể loại " + form.getGenreName());
        return "redirect:/admin/genres";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", genreService.getForm(id));
        return FORM_VIEW;
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") GenreForm form, BindingResult br,
                         Model model, RedirectAttributes ra) {
        form.setGenreId(id);
        if (br.hasErrors()) {
            return FORM_VIEW;
        }
        try {
            genreService.update(id, form);
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return FORM_VIEW;
        }
        ra.addFlashAttribute("success", "Đã cập nhật thể loại");
        return "redirect:/admin/genres";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        genreService.delete(id); // BusinessException -> GlobalExceptionHandler (flash error)
        ra.addFlashAttribute("success", "Đã xoá thể loại");
        return "redirect:/admin/genres";
    }
}