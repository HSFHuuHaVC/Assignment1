package com.fu.cinema.service;

import com.fu.cinema.dto.RegisterForm;
import com.fu.cinema.dto.SessionUser;

public interface AuthService {

    SessionUser login(String username, String password);

    SessionUser register(RegisterForm form);
}
