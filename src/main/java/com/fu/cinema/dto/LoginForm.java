package com.fu.cinema.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

@Data
public class LoginForm {
    @NotBlank(message = "Vui lòng nhập tên đăng nhập")
    @Size(max = 100, message = "Tên đăng nhập tối đa 100 ký tự")
    private String username;

    @NotBlank(message = "Vui lòng nhập mật khẩu")
    @ToString.Exclude
    private String password;
}
