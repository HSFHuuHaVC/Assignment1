package com.fu.cinema.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " không tồn tại (id = " + id + ")");
    }
}
