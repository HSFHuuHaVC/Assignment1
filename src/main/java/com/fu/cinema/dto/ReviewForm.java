package com.fu.cinema.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ReviewForm {

    @NotNull(message = "Vui lòng chọn số sao")
    @Min(1)
    @Max(5)
    private Integer rating;

    @NotBlank(message = "Vui lòng nhập nhận xét")
    @Size(max = 1000)
    private String comment;
}
