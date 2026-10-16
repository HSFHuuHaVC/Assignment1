package com.fu.cinema.dto;

import com.fu.cinema.enums.Role;
import lombok.Value;

import java.io.Serializable;

// Thông tin người dùng lưu trong HttpSession (KHÔNG lưu mật khẩu)
@Value
public class SessionUser implements Serializable {

    private static final long serialVersionUID = 1L;

    Long accountId;
    Long customerId;          // null nếu là ADMIN
    String displayName;
    Role role;

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isCustomer() {
        return role == Role.CUSTOMER;
    }
}
