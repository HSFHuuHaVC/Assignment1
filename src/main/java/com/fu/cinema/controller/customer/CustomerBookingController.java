package com.fu.cinema.controller.customer;

import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.service.SeatMapService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerBookingController {
    private final SeatMapService seatMapService;

    @GetMapping("/showtimes/{id}/seats")
    public String seats(@PathVariable Long id, Model model) {
        try {
            model.addAttribute("seatMap", seatMapService.getSeatMap(id));
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
        }
        return "customer/seats";
    }
}
