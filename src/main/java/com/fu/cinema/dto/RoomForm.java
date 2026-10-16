package com.fu.cinema.dto;

import com.fu.cinema.enums.RoomStatus;
import com.fu.cinema.enums.RoomType;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RoomForm {

    private Long roomId;

    @NotBlank(message = "Tên phòng không được để trống")
    @Size(max = 50)
    private String roomName;

    @NotNull(message = "Vui lòng chọn loại phòng")
    private RoomType roomType;

    @NotNull(message = "Số hàng không được để trống")
    @Min(value = 1, message = "Số hàng từ 1 đến 26")
    @Max(value = 26, message = "Số hàng từ 1 đến 26")
    private Integer totalRows;

    @NotNull(message = "Số ghế mỗi hàng không được để trống")
    @Min(value = 1, message = "Số ghế mỗi hàng từ 1 đến 30")
    @Max(value = 30, message = "Số ghế mỗi hàng từ 1 đến 30")
    private Integer seatsPerRow;

    @NotNull(message = "Vui lòng chọn trạng thái")
    private RoomStatus status;
}