package com.fu.cinema.controller.customer;

import com.fu.cinema.dto.ReviewForm;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.service.ReviewService;
import com.fu.cinema.util.SessionUtils;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/customer/movies")
@RequiredArgsConstructor
public class CustomerReviewController {

    private final ReviewService reviewService;

    @PostMapping("/{movieId}/review")
    public String review(@PathVariable Long movieId, @Valid @ModelAttribute("reviewForm") ReviewForm form,
                         BindingResult br, HttpSession session, RedirectAttributes ra) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("error", br.getAllErrors().get(0).getDefaultMessage());
        } else {
            try {
                reviewService.save(SessionUtils.getCurrentCustomerId(session), movieId, form);
                ra.addFlashAttribute("success", "Cảm ơn bạn đã đánh giá!");
            } catch (BusinessException e) {
                ra.addFlashAttribute("error", e.getMessage());
            }
        }
        return "redirect:/movies/" + movieId;
    }
}
