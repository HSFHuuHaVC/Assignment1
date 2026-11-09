package com.fu.cinema.desktop;

import com.fu.cinema.desktop.model.PageResult;
import com.fu.cinema.desktop.util.AlertUtil;
import com.fu.cinema.desktop.util.FxValidator;
import com.fu.cinema.desktop.util.MoneyUtil;
import com.fu.cinema.dto.MovieForm;
import com.fu.cinema.exception.BusinessException;
import jakarta.validation.Validation;
import javafx.scene.control.Alert;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.transaction.CannotCreateTransactionException;
import java.math.BigDecimal;
import java.util.concurrent.CompletionException;
import static org.junit.jupiter.api.Assertions.*;

class DesktopUtilityTests {
    @Test void emptyPageHasOnePageIndicatorAndImmutableRows() {
        var page = PageResult.of(Page.<String>empty(), String::toUpperCase);
        assertEquals(1, page.totalPages());
        assertEquals(0, page.page());
        assertThrows(UnsupportedOperationException.class, () -> page.items().add("X"));
    }
    @Test void classifiesWrappedBusinessAndConnectionErrorsWithoutExposingDetails() {
        var business = AlertUtil.describe(new CompletionException(new BusinessException("Ghế đã có người đặt")));
        assertEquals(Alert.AlertType.WARNING, business.type());
        assertEquals("Ghế đã có người đặt", business.text());
        var unavailable = AlertUtil.describe(new CannotCreateTransactionException("private connection details"));
        assertTrue(unavailable.text().contains("Không kết nối"));
        assertFalse(unavailable.text().contains("private"));
    }
    @Test void usesExistingMovieFormConstraints() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = new FxValidator(factory.getValidator());
            var form = new MovieForm();
            form.setDuration(20);
            assertTrue(validator.validate(form).containsKey("duration"));
            assertTrue(validator.validate(form).containsKey("title"));
        }
    }
    @Test void formatsVietnameseMoney() {
        assertEquals("1.234.000 đ", MoneyUtil.format(new BigDecimal("1234000")));
        assertEquals("", MoneyUtil.format(null));
    }
}
