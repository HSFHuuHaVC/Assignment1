package com.fu.cinema.service;

import com.fu.cinema.dto.ReportResult;

import java.time.LocalDate;

public interface ReportService {

    ReportResult generate(LocalDate startDate, LocalDate endDate);
}
