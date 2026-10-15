package com.fu.cinema.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.ToString;

@Data
public class ChangePasswordForm {

    @NotBlank(message = "Vui lòng nhập mật khẩu hiện tại")
    @ToString.Exclude
    private String currentPassword;

    @NotBlank(message = "Vui lòng nhập mật khẩu mới")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
             message = "Mật khẩu ≥ 8 ký tự, có chữ hoa, chữ thường và chữ số")
    @ToString.Exclude
    private String newPassword;

    @NotBlank(message = "Vui lòng nhập lại mật khẩu mới")
    @ToString.Exclude
    private String confirmPassword;
}
