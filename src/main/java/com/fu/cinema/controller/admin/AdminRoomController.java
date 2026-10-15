package com.fu.cinema.controller.admin;

import com.fu.cinema.dto.RoomForm;
import com.fu.cinema.enums.RoomStatus;
import com.fu.cinema.enums.RoomType;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/rooms")
@RequiredArgsConstructor
public class AdminRoomController {

    private static final String FORM_VIEW = "admin/rooms/form";

    private final RoomService roomService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("rooms", roomService.findAll());
        return "admin/rooms/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        RoomForm form = new RoomForm();
        form.setStatus(RoomStatus.ACTIVE);
        return showForm(form, model);
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("form") RoomForm form, BindingResult br,
                         Model model, RedirectAttributes ra) {
        if (br.hasErrors()) {
            return showForm(form, model);
        }
        try {
            roomService.create(form);
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return showForm(form, model);
        }
        ra.addFlashAttribute("success", "Đã thêm phòng " + form.getRoomName());
        return "redirect:/admin/rooms";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        return showForm(roomService.getForm(id), model);
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") RoomForm form, BindingResult br,
                         Model model, RedirectAttributes ra) {
        form.setRoomId(id);
        if (br.hasErrors()) {
            return showForm(form, model);
        }
        try {
            roomService.update(id, form);
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return showForm(form, model);
        }
        ra.addFlashAttribute("success", "Đã cập nhật phòng " + form.getRoomName());
        return "redirect:/admin/rooms";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        boolean deleted = roomService.delete(id);
        ra.addFlashAttribute("success", deleted
                ? "Đã xoá phòng"
                : "Phòng đã có suất chiếu nên được chuyển sang trạng thái MAINTENANCE");
        return "redirect:/admin/rooms";
    }

    private String showForm(RoomForm form, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("roomTypes", RoomType.values());
        model.addAttribute("statuses", RoomStatus.values());
        return FORM_VIEW;
    }
}