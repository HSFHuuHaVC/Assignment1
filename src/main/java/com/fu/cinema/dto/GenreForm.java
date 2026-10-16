package com.fu.cinema.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GenreForm {

    private Long genreId;

    @NotBlank(message = "Tên thể loại không được để trống")
    @Size(max = 50, message = "Tên thể loại tối đa 50 ký tự")
    private String genreName;

    @NotBlank(message = "Mô tả không được để trống")
    @Size(max = 500)
    private String description;
}
