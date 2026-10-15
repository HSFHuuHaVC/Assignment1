package com.fu.cinema;

import com.fu.cinema.dto.*;
import com.fu.cinema.enums.Role;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

class ContractValidationTests {
    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private final Validator validator = FACTORY.getValidator();

    @AfterAll
    static void closeValidatorFactory() {
        FACTORY.close();
    }

    @Test
    void registrationAcceptsValidInputAndRejectsInvalidMobileAndIdentityCard() {
        RegisterForm form = new RegisterForm();
        form.setFullName("Nguyễn Văn An");
        form.setEmail("an@example.test");
        form.setMobile("0912345678");
        form.setIdentityCard("012345678901");
        form.setBirthday(LocalDate.of(2000, 1, 1));
        form.setPassword("Example@123");
        form.setConfirmPassword("Example@123");
        assertThat(validator.validate(form)).isEmpty();
        form.setMobile("1234567890");
        form.setIdentityCard("not-a-number");
        assertThat(validator.validate(form)).extracting(v -> v.getPropertyPath().toString())
                .contains("mobile", "identityCard");
    }

    @Test
    void passwordFormsRejectWeakValuesAndDoNotPrintPasswords() {
        ChangePasswordForm form = new ChangePasswordForm();
        form.setCurrentPassword("private-current");
        form.setNewPassword("weak");
        form.setConfirmPassword("private-confirm");
        assertThat(validator.validate(form)).extracting(v -> v.getPropertyPath().toString())
                .contains("newPassword");
        assertThat(form.toString()).doesNotContain("private-current", "weak", "private-confirm");
        form.setNewPassword("Example@123");
        assertThat(validator.validate(form)).isEmpty();
    }

    @Test
    void bookingContractRequiresShowtimeAndOneToEightSeats() {
        BookingRequest form = new BookingRequest();
        assertThat(validator.validate(form)).extracting(v -> v.getPropertyPath().toString())
                .contains("showtimeId", "seatCodes");
        form.setShowtimeId(1L);
        form.setSeatCodes(List.of("A1"));
        assertThat(validator.validate(form)).isEmpty();
        form.setSeatCodes(IntStream.rangeClosed(1, 9).mapToObj(i -> "A" + i).toList());
        assertThat(validator.validate(form)).extracting(v -> v.getPropertyPath().toString())
                .containsExactly("seatCodes");
    }

    @Test
    void roomContractRejectsDimensionsOutsideSupportedSeatMap() {
        RoomForm form = new RoomForm();
        form.setTotalRows(27);
        form.setSeatsPerRow(31);
        assertThat(validator.validate(form)).extracting(v -> v.getPropertyPath().toString())
                .contains("totalRows", "seatsPerRow");
    }

    @Test
    void sessionUserSerializesOnlyPublicSessionInformation() throws Exception {
        SessionUser user = new SessionUser(1L, 2L, "An", Role.CUSTOMER);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(user);
        }
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            assertThat(input.readObject()).isEqualTo(user);
        }
        assertThat(user.isCustomer()).isTrue();
        assertThat(user.isAdmin()).isFalse();
        assertThat(SessionUser.class.getDeclaredFields()).extracting(java.lang.reflect.Field::getName)
                .doesNotContain("password", "passwordHash", "confirmPassword");
    }
}
