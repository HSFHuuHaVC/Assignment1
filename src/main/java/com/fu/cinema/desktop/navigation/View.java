package com.fu.cinema.desktop.navigation;

public enum View {
    LOGIN("login.fxml", "Đăng nhập"),
    MAIN("main.fxml", "FU Cinema — Quầy vé"),
    DASHBOARD("dashboard.fxml", "Dashboard"),
    GENRE("genre.fxml", "Thể loại"),
    MOVIE("movie.fxml", "Phim"),
    MOVIE_DIALOG("movie-dialog.fxml", "Phim"),
    SCHEDULE("schedule.fxml", "Phòng & Suất chiếu"),
    COUNTER_SALE("counter-sale.fxml", "Bán vé tại quầy"),
    BOOKING("booking.fxml", "Đơn đặt vé"),
    REPORT("report.fxml", "Báo cáo doanh thu");

    private final String file;
    private final String title;
    View(String file, String title) { this.file = file; this.title = title; }
    public String path() { return "/desktop/fxml/" + file; }
    public String title() { return title; }
}
