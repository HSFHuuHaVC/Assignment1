package com.fu.cinema.dto;

import com.fu.cinema.enums.AccountStatus;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

// Form Admin thêm/sửa khách hàng. Mật khẩu bắt buộc khi thêm, để trống khi sửa = giữ nguyên
@Data
public class CustomerForm {

    private Long customerId;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 100)
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 100, message = "Email tối đa 100 ký tự")
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại gồm 10 chữ số, bắt đầu bằng 0")
    private String mobile;

    @NotNull(message = "Ngày sinh không được để trống")
    @Past(message = "Ngày sinh phải ở quá khứ")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate birthday;

    @NotBlank(message = "CCCD không được để trống")
    @Pattern(regexp = "^\\d{12}$", message = "CCCD gồm đúng 12 chữ số")
    private String identityCard;

    @Pattern(regexp = "^$|^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
             message = "Mật khẩu ≥ 8 ký tự, có chữ hoa, chữ thường và chữ số")
    @ToString.Exclude
    private String password;

    @NotNull
    private AccountStatus status = AccountStatus.ACTIVE;
}
