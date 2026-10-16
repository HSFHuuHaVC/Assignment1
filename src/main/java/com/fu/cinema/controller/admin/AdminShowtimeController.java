package com.fu.cinema.controller.admin;

import com.fu.cinema.dto.ShowtimeForm;
import com.fu.cinema.enums.ShowtimeStatus;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.service.MovieService;
import com.fu.cinema.service.RoomService;
import com.fu.cinema.service.ShowtimeService;
import com.fu.cinema.util.PageUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin/showtimes")
@RequiredArgsConstructor
public class AdminShowtimeController {

    private static final String FORM_VIEW = "admin/showtimes/form";

    private final ShowtimeService showtimeService;
    private final MovieService movieService;
    private final RoomService roomService;

    @GetMapping
    public String list(@RequestParam(required = false) Long movieId,
                       @RequestParam(required = false) Long roomId,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("showtimes", showtimeService.search(movieId, roomId, date, page, PageUtils.DEFAULT_SIZE));
        model.addAttribute("movies", movieService.findSchedulable());
        model.addAttribute("rooms", roomService.findAll());
        model.addAttribute("movieId", movieId);
        model.addAttribute("roomId", roomId);
        model.addAttribute("date", date);
        model.addAttribute("pageUrl", PageUtils.currentUrlWithoutPage());
        return "admin/showtimes/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        return showForm(new ShowtimeForm(), model);
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("form") ShowtimeForm form, BindingResult br,
                         Model model, RedirectAttributes ra) {
        if (br.hasErrors()) {
            return showForm(form, model);
        }
        try {
            showtimeService.create(form);
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return showForm(form, model);
        }
        ra.addFlashAttribute("success", "Đã thêm suất chiếu");
        return "redirect:/admin/showtimes";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        return showForm(showtimeService.getForm(id), model);
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") ShowtimeForm form, BindingResult br,
                         Model model, RedirectAttributes ra) {
        form.setShowtimeId(id);
        if (br.hasErrors()) {
            return showForm(form, model);
        }
        try {
            showtimeService.update(id, form);
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return showForm(form, model);
        }
        ra.addFlashAttribute("success", "Đã cập nhật suất chiếu");
        return "redirect:/admin/showtimes";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        boolean deleted = showtimeService.delete(id);
        ra.addFlashAttribute("success", deleted
                ? "Đã xoá suất chiếu"
                : "Suất chiếu đã có đơn đặt vé: đã huỷ suất chiếu và toàn bộ đơn liên quan");
        return "redirect:/admin/showtimes";
    }

    private String showForm(ShowtimeForm form, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("movies", movieService.findSchedulable());
        model.addAttribute("rooms", roomService.findActive());
        model.addAttribute("statuses", ShowtimeStatus.values());
        return FORM_VIEW;
    }
}