package com.fu.cinema.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BookingRequest {

    @NotNull
    private Long showtimeId;

    @NotEmpty(message = "Vui lòng chọn ít nhất 1 ghế")
    @Size(max = 8, message = "Mỗi đơn tối đa 8 ghế")
    private List<String> seatCodes = new ArrayList<>();
}
