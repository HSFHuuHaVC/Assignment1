package com.fu.cinema.controller.customer;

import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.exception.ResourceNotFoundException;
import com.fu.cinema.dto.BookingRequest;
import com.fu.cinema.enums.BookingStatus;
import com.fu.cinema.service.BookingService;
import com.fu.cinema.service.SeatMapService;
import com.fu.cinema.util.SessionUtils;
import com.fu.cinema.util.PageUtils;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.access.AccessDeniedException;

@Controller
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerBookingController {
    private final SeatMapService seatMapService;
    private final BookingService bookingService;

    @InitBinder("bookingRequest")
    public void configureBooking(WebDataBinder binder) {
        binder.setAllowedFields("showtimeId", "seatCodes", "seatCodes[*]");
        binder.setAutoGrowCollectionLimit(32);
    }

    @GetMapping("/showtimes/{id}/seats")
    public String seats(@PathVariable Long id, Model model) {
        try {
            model.addAttribute("seatMap", seatMapService.getSeatMap(id));
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
        }
        return "customer/seats";
    }

    @PostMapping("/bookings")
    public String book(@Valid @ModelAttribute("bookingRequest") BookingRequest request, BindingResult binding,
                       HttpSession session, RedirectAttributes flash) {
        Long customerId = requireCustomerId(session);
        if (request.getShowtimeId() == null || binding.hasFieldErrors("showtimeId")) {
            throw new ResourceNotFoundException("Suất chiếu", request.getShowtimeId());
        }
        String back = "redirect:/customer/showtimes/" + request.getShowtimeId() + "/seats";
        if (binding.hasErrors()) {
            flash.addFlashAttribute("error", binding.getAllErrors().getFirst().getDefaultMessage());
            return back;
        }
        try {
            var booking = bookingService.create(customerId, request);
            flash.addFlashAttribute("success", "Đặt vé thành công! Mã đơn #" + booking.getBookingId());
            return "redirect:/customer/bookings/" + booking.getBookingId();
        } catch (BusinessException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
            return back;
        }
    }

    @GetMapping("/bookings")
    public String history(@RequestParam(required = false) BookingStatus status,
                          @RequestParam(defaultValue = "0") int page, HttpSession session, Model model) {
        model.addAttribute("bookings", bookingService.findByCustomer(requireCustomerId(session),
                status, page, PageUtils.DEFAULT_SIZE));
        model.addAttribute("statuses", BookingStatus.values());
        model.addAttribute("status", status);
        model.addAttribute("pageUrl", PageUtils.currentUrlWithoutPage());
        return "customer/bookings";
    }

    @GetMapping("/bookings/{id}")
    public String detail(@PathVariable Long id, HttpSession session, Model model) {
        model.addAttribute("booking", bookingService.getDetailForCustomer(id, requireCustomerId(session)));
        return "customer/booking-detail";
    }

    @PostMapping("/bookings/{id}/cancel")
    public String cancel(@PathVariable Long id, HttpSession session, RedirectAttributes flash) {
        try {
            bookingService.cancelByCustomer(id, requireCustomerId(session));
            flash.addFlashAttribute("success", "Đã huỷ đơn #" + id);
        } catch (BusinessException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/customer/bookings/" + id;
    }

    private Long requireCustomerId(HttpSession session) {
        Long id = SessionUtils.getCurrentCustomerId(session);
        if (id == null) throw new AccessDeniedException("Vui lòng đăng nhập bằng tài khoản khách hàng");
        return id;
    }
}
