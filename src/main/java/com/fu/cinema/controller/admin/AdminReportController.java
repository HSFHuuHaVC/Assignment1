package com.fu.cinema.controller.admin;

import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportService reportService;

    @GetMapping
    public String report(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                         Model model) {
        // Mặc định: từ đầu tháng đến hôm nay
        LocalDate start = startDate != null ? startDate : LocalDate.now().withDayOfMonth(1);
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        model.addAttribute("startDate", start);
        model.addAttribute("endDate", end);
        try {
            model.addAttribute("report", reportService.generate(start, end));
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "admin/report";
    }
}
