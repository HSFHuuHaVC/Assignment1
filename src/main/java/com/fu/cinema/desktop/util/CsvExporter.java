package com.fu.cinema.desktop.util;

import com.fu.cinema.desktop.model.ReportBookingRow;
import com.fu.cinema.desktop.model.ReportData;
import com.fu.cinema.desktop.model.RevenueRow;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CsvExporter {

    private CsvExporter() {
    }

    public static void write(Path path, ReportData d) {
        try (BufferedWriter w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            w.write('\uFEFF'); // BOM -> Excel đọc đúng tiếng Việt UTF-8
            w.write("Báo cáo doanh thu," + d.start() + "," + d.end());
            w.newLine();
            w.write("Phim,Số vé,Doanh thu");
            w.newLine();
            for (RevenueRow r : d.revenue()) {
                w.write(String.join(",", csv(r.title()), String.valueOf(r.tickets()), r.revenue().toPlainString()));
                w.newLine();
            }
            w.newLine();
            w.write("Mã đơn,Ngày đặt,Khách hàng,Phim,Tổng tiền");
            w.newLine();
            for (ReportBookingRow b : d.bookings()) {
                w.write(String.join(",", b.id().toString(), b.bookingDate().toString(), csv(b.customerName()),
                        csv(b.movieTitle()), b.total().toPlainString()));
                w.newLine();
            }
            w.write("TỔNG,%d đơn,%d vé,,%s".formatted(d.totalBookings(), d.totalTickets(), d.totalRevenue().toPlainString()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String csv(String v) {
        if (v == null) return "";
        return v.contains(",") || v.contains("\"") || v.contains("\n") ? "\"" + v.replace("\"", "\"\"") + "\"" : v;
    }
}

