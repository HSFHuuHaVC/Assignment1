package com.fu.cinema.exception;

// Vi phạm quy tắc nghiệp vụ (BR-xx) — thông báo hiển thị trực tiếp cho người dùng
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
