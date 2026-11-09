package com.fu.cinema.desktop;

import com.fu.cinema.desktop.facade.ReportFacade;
import com.fu.cinema.desktop.model.ReportBookingRow;
import com.fu.cinema.desktop.model.ReportData;
import com.fu.cinema.desktop.model.RevenueRow;
import com.fu.cinema.desktop.util.CsvExporter;
import com.fu.cinema.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.datasource.url=jdbc:h2:mem:desktopReports;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@ActiveProfiles({"test", "desktop"})
class ReportFacadeTests {

    @Autowired private ReportFacade reportFacade;

    @Test
    void generateReport_returnsData() {
        LocalDate start = LocalDate.now().minusMonths(1);
        LocalDate end = LocalDate.now();

        ReportData data = reportFacade.generate(start, end);
        assertNotNull(data);
        assertEquals(start, data.start());
        assertEquals(end, data.end());
        assertNotNull(data.revenue());
        assertNotNull(data.bookings());
        assertTrue(data.totalBookings() >= 0);
        assertTrue(data.totalTickets() >= 0);
        assertNotNull(data.totalRevenue());
    }

    @Test
    void generateReport_startAfterEnd_throwsException() {
        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now().minusDays(5);

        assertThrows(BusinessException.class, () -> reportFacade.generate(start, end));
    }

    @Test
    void csvExporter_writesValidCsvFile(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("test-report.csv");

        List<RevenueRow> revenue = List.of(
                new RevenueRow("Avengers: Endgame", 100, new BigDecimal("10000000"))
        );
        List<ReportBookingRow> bookings = List.of(
                new ReportBookingRow(1L, LocalDateTime.now(), "Nguyễn Văn A", "Avengers: Endgame", new BigDecimal("100000"))
        );
        ReportData reportData = new ReportData(
                LocalDate.now().minusDays(7),
                LocalDate.now(),
                revenue,
                bookings,
                1,
                100,
                new BigDecimal("10000000")
        );

        CsvExporter.write(csvFile, reportData);

        assertTrue(Files.exists(csvFile));
        List<String> lines = Files.readAllLines(csvFile);
        assertFalse(lines.isEmpty());
        assertTrue(lines.get(0).contains("Báo cáo doanh thu"));
        assertTrue(lines.stream().anyMatch(l -> l.contains("Avengers: Endgame")));
    }
}

