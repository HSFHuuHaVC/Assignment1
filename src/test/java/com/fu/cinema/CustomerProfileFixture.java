package com.fu.cinema;

import com.fu.cinema.dto.RegisterForm;
import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.service.AuthService;
import java.time.LocalDate;

public final class CustomerProfileFixture {
    private CustomerProfileFixture() {}
    public record Data(SessionUser an, SessionUser binh) {}
    public static Data seed(AuthService auth) {
        return new Data(auth.register(form(true)), auth.register(form(false)));
    }
    private static RegisterForm form(boolean first) {
        RegisterForm form = new RegisterForm();
        form.setFullName(first ? "Nguyen Van An" : "Tran Thi Binh");
        form.setEmail(first ? "an@example.test" : "binh@example.test");
        form.setMobile(first ? "0901234567" : "0912345678");
        form.setBirthday(LocalDate.of(2000, 1, 1));
        form.setIdentityCard(first ? "012345678901" : "112345678901");
        form.setPassword("Example@123");
        form.setConfirmPassword("Example@123");
        return form;
    }
}
