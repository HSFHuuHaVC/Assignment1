package com.fu.cinema.dto;

import com.fu.cinema.enums.AgeRating;
import com.fu.cinema.enums.MovieStatus;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class MovieForm {

    private Long movieId;

    @NotBlank(message = "Tên phim không được để trống")
    @Size(max = 200)
    private String title;

    @NotBlank(message = "Đạo diễn không được để trống")
    @Size(max = 100)
    private String director;

    @NotNull(message = "Thời lượng không được để trống")
    @Min(value = 30, message = "Thời lượng tối thiểu 30 phút")
    @Max(value = 300, message = "Thời lượng tối đa 300 phút")
    private Integer duration;

    @NotNull(message = "Ngày khởi chiếu không được để trống")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate releaseDate;

    @NotNull(message = "Vui lòng chọn độ tuổi")
    private AgeRating ageRating;

    @NotBlank(message = "Mô tả không được để trống")
    @Size(max = 2000)
    private String description;

    @NotBlank(message = "Poster URL không được để trống")
    @Size(max = 500)
    private String posterUrl;

    @NotNull(message = "Vui lòng chọn thể loại")
    private Long genreId;

    @NotNull(message = "Vui lòng chọn trạng thái")
    private MovieStatus status;
}
