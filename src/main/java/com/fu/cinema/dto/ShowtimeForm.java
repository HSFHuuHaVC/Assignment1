package com.fu.cinema.dto;

import com.fu.cinema.enums.ShowtimeStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ShowtimeForm {

    private Long showtimeId;

    @NotNull(message = "Vui lòng chọn phim")
    private Long movieId;

    @NotNull(message = "Vui lòng chọn phòng")
    private Long roomId;

    @NotNull(message = "Giờ bắt đầu không được để trống")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")          // khớp định dạng của <input type="datetime-local">
    private LocalDateTime startTime;

    // Để trống → hệ thống tự tính = StartTime + Duration + 15 phút dọn phòng
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endTime;

    @NotNull(message = "Giá vé không được để trống")
    @DecimalMin(value = "1000", message = "Giá vé tối thiểu 1.000đ")
    private BigDecimal ticketPrice;

    @NotNull
    private ShowtimeStatus status = ShowtimeStatus.OPEN;
}
