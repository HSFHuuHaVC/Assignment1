package com.fu.cinema.service.impl;

import com.fu.cinema.dto.RegisterForm;
import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.entity.Account;
import com.fu.cinema.entity.Customer;
import com.fu.cinema.enums.AccountStatus;
import com.fu.cinema.enums.Role;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.repository.AccountRepository;
import com.fu.cinema.repository.CustomerRepository;
import com.fu.cinema.service.AuthService;
import com.fu.cinema.util.ValidationUtils;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {
    private static final int MIN_REGISTER_AGE = 13;
    private static final String INVALID_LOGIN = "Tên đăng nhập hoặc mật khẩu không đúng";

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final Validator validator;

    @Override
    public SessionUser login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isEmpty()
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException(INVALID_LOGIN);
        }
        Account account = accountRepository.findByUsername(normalizeEmail(username))
                .filter(value -> passwordEncoder.matches(password, value.getPassword()))
                .orElseThrow(() -> new BusinessException(INVALID_LOGIN));

        if (account.getRole() == Role.ADMIN) {
            return new SessionUser(account.getAccountId(), null, "Administrator", Role.ADMIN);
        }
        Customer customer = customerRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy hồ sơ khách hàng"));
        return new SessionUser(account.getAccountId(), customer.getCustomerId(),
                customer.getFullName(), Role.CUSTOMER);
    }

    @Override
    @Transactional
    public SessionUser register(RegisterForm form) {
        if (form == null) {
            throw new BusinessException("Vui lòng nhập thông tin đăng ký");
        }
        form.setEmail(form.getEmail() == null ? null : normalizeEmail(form.getEmail()));
        form.setFullName(ValidationUtils.blankToNull(form.getFullName()));
        form.setMobile(ValidationUtils.blankToNull(form.getMobile()));
        form.setIdentityCard(ValidationUtils.blankToNull(form.getIdentityCard()));
        validator.validate(form).stream()
                .sorted(Comparator.comparing(value -> value.getPropertyPath().toString()))
                .findFirst().ifPresent(value -> { throw new BusinessException(value.getMessage()); });
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            throw new BusinessException("Mật khẩu nhập lại không khớp");
        }
        if (form.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException("Mật khẩu quá dài. Vui lòng chọn mật khẩu ngắn hơn");
        }
        if (ValidationUtils.age(form.getBirthday()) < MIN_REGISTER_AGE) {
            throw new BusinessException("Bạn phải từ đủ 13 tuổi để đăng ký");
        }
        String email = form.getEmail();
        if (accountRepository.existsByUsername(email) || customerRepository.existsByEmail(email)) {
            throw new BusinessException("Email đã được sử dụng");
        }
        if (customerRepository.existsByMobile(form.getMobile())) {
            throw new BusinessException("Số điện thoại đã được sử dụng");
        }
        if (customerRepository.existsByIdentityCard(form.getIdentityCard())) {
            throw new BusinessException("Số CCCD đã được sử dụng");
        }
        try {
            Account account = accountRepository.save(new Account(email,
                    passwordEncoder.encode(form.getPassword()), Role.CUSTOMER, AccountStatus.ACTIVE));
            Customer customer = new Customer();
            customer.setFullName(form.getFullName());
            customer.setEmail(email);
            customer.setMobile(form.getMobile());
            customer.setBirthday(form.getBirthday());
            customer.setIdentityCard(form.getIdentityCard());
            customer.setAccount(account);
            customerRepository.saveAndFlush(customer);
            return new SessionUser(account.getAccountId(), customer.getCustomerId(),
                    customer.getFullName(), Role.CUSTOMER);
        } catch (DataIntegrityViolationException ex) {
            // A concurrent registration may win after the duplicate checks.
            throw new BusinessException("Thông tin đăng ký đã được sử dụng. Vui lòng kiểm tra lại");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
