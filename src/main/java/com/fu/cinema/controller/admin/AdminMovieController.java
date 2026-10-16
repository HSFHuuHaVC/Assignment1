package com.fu.cinema.controller.admin;

import com.fu.cinema.dto.MovieForm;
import com.fu.cinema.enums.AgeRating;
import com.fu.cinema.enums.MovieStatus;
import com.fu.cinema.service.GenreService;
import com.fu.cinema.service.MovieService;
import com.fu.cinema.util.PageUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/movies")
@RequiredArgsConstructor
public class AdminMovieController {

    private static final String FORM_VIEW = "admin/movies/form";

    private final MovieService movieService;
    private final GenreService genreService;

    @GetMapping
    public String list(@RequestParam(required = false) String kw,
                       @RequestParam(required = false) Long genreId,
                       @RequestParam(required = false) MovieStatus status,
                       @RequestParam(defaultValue = "releaseDate") String sort,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("movies", movieService.searchForAdmin(kw, genreId, status, page, PageUtils.DEFAULT_SIZE, sort));
        model.addAttribute("genres", genreService.findAll());
        model.addAttribute("statuses", MovieStatus.values());
        model.addAttribute("kw", kw);
        model.addAttribute("genreId", genreId);
        model.addAttribute("status", status);
        model.addAttribute("sort", sort);
        model.addAttribute("pageUrl", PageUtils.currentUrlWithoutPage());
        return "admin/movies/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        MovieForm form = new MovieForm();
        form.setStatus(MovieStatus.COMING_SOON);
        return showForm(form, model);
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("form") MovieForm form, BindingResult br,
                         Model model, RedirectAttributes ra) {
        if (br.hasErrors()) {
            return showForm(form, model);
        }
        movieService.create(form);
        ra.addFlashAttribute("success", "Đã thêm phim " + form.getTitle());
        return "redirect:/admin/movies";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        return showForm(movieService.getForm(id), model);
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") MovieForm form, BindingResult br,
                         Model model, RedirectAttributes ra) {
        form.setMovieId(id);
        if (br.hasErrors()) {
            return showForm(form, model);
        }
        movieService.update(id, form);
        ra.addFlashAttribute("success", "Đã cập nhật phim " + form.getTitle());
        return "redirect:/admin/movies";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        boolean deleted = movieService.delete(id);
        ra.addFlashAttribute("success", deleted
                ? "Đã xoá phim"
                : "Phim đã có suất chiếu nên được chuyển sang trạng thái STOPPED");
        return "redirect:/admin/movies";
    }

    private String showForm(MovieForm form, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("genres", genreService.findAll());
        model.addAttribute("ageRatings", AgeRating.values());
        model.addAttribute("statuses", MovieStatus.values());
        return FORM_VIEW;
    }
}