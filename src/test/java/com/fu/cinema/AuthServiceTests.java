package com.fu.cinema;

import com.fu.cinema.dto.LoginForm;
import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.entity.Account;
import com.fu.cinema.entity.Customer;
import com.fu.cinema.enums.AccountStatus;
import com.fu.cinema.enums.Role;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.repository.AccountRepository;
import com.fu.cinema.repository.CustomerRepository;
import com.fu.cinema.service.impl.AuthServiceImpl;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {
    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    @Mock AccountRepository accounts;
    @Mock CustomerRepository customers;
    @Mock PasswordEncoder encoder;
    private AuthServiceImpl auth;

    @BeforeEach
    void setup() {
        auth = new AuthServiceImpl(accounts, customers, encoder, FACTORY.getValidator());
    }

    @AfterAll
    static void closeFactory() {
        FACTORY.close();
    }

    private Account account(Role role) {
        Account account = new Account("user@example.test", "bcrypt-hash", role, AccountStatus.ACTIVE);
        account.setAccountId(7L);
        return account;
    }

    @Test
    void adminLoginNormalizesUsernameAndUsesPasswordMatches() {
        when(accounts.findByUsername("user@example.test")).thenReturn(Optional.of(account(Role.ADMIN)));
        when(encoder.matches("Example@123", "bcrypt-hash")).thenReturn(true);
        SessionUser user = auth.login(" USER@EXAMPLE.TEST ", "Example@123");
        assertThat(user.isAdmin()).isTrue();
        assertThat(user.getAccountId()).isEqualTo(7L);
        assertThat(user.getCustomerId()).isNull();
        verifyNoInteractions(customers);
    }

    @Test
    void customerLoginUsesIdentityAndDisplayNameFromStoredProfile() {
        Account account = account(Role.CUSTOMER);
        Customer customer = new Customer();
        customer.setCustomerId(8L);
        customer.setFullName("Nguyễn Văn An");
        when(accounts.findByUsername(account.getUsername())).thenReturn(Optional.of(account));
        when(encoder.matches("Example@123", account.getPassword())).thenReturn(true);
        when(customers.findByAccount_AccountId(7L)).thenReturn(Optional.of(customer));
        assertThat(auth.login(account.getUsername(), "Example@123"))
                .isEqualTo(new SessionUser(7L, 8L, "Nguyễn Văn An", Role.CUSTOMER));
    }

    @Test
    void missingAccountAndWrongPasswordUseTheSameErrorMessage() {
        when(accounts.findByUsername("missing@example.test")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> auth.login("missing@example.test", "Wrong@123"))
                .isInstanceOf(BusinessException.class).hasMessage("Tên đăng nhập hoặc mật khẩu không đúng");
        when(accounts.findByUsername("user@example.test")).thenReturn(Optional.of(account(Role.ADMIN)));
        assertThatThrownBy(() -> auth.login("user@example.test", "Wrong@123"))
                .isInstanceOf(BusinessException.class).hasMessage("Tên đăng nhập hoặc mật khẩu không đúng");
    }

    @Test
    void blankOrOversizedCredentialsAreRejectedWithoutEncoderErrors() {
        assertThatThrownBy(() -> auth.login(null, "x")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> auth.login(" ", "x")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> auth.login("user@example.test", null)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> auth.login("user@example.test", "x".repeat(73))).isInstanceOf(BusinessException.class);
        verifyNoInteractions(accounts, encoder);
    }

    @Test
    void customerWithoutProfileCannotCreateSessionIdentity() {
        when(accounts.findByUsername("user@example.test")).thenReturn(Optional.of(account(Role.CUSTOMER)));
        when(encoder.matches("Example@123", "bcrypt-hash")).thenReturn(true);
        when(customers.findByAccount_AccountId(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> auth.login("user@example.test", "Example@123"))
                .isInstanceOf(BusinessException.class).hasMessage("Không tìm thấy hồ sơ khách hàng");
    }

    @Test
    void lockedAccountCannotLoginEvenWithCorrectPassword() {
        Account account = account(Role.CUSTOMER);
        account.setStatus(AccountStatus.LOCKED);
        when(accounts.findByUsername(account.getUsername())).thenReturn(Optional.of(account));
        when(encoder.matches("Example@123", account.getPassword())).thenReturn(true);
        assertThatThrownBy(() -> auth.login(account.getUsername(), "Example@123"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("bị khóa");
        verifyNoInteractions(customers);
    }

    @Test
    void loginFormDoesNotPrintPasswords() {
        LoginForm form = new LoginForm();
        form.setPassword("private-password");
        assertThat(form.toString()).doesNotContain("private-password");
    }
}
