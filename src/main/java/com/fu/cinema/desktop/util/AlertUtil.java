package com.fu.cinema.desktop.util;

import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.exception.ResourceNotFoundException;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.CannotCreateTransactionException;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

@Slf4j
public final class AlertUtil {
    private AlertUtil() { }

    public static void showError(Throwable error) {
        ErrorMessage message = describe(error);
        show(message.type(), message.type() == Alert.AlertType.WARNING ? "Không thực hiện được" : "Lỗi", message.text());
    }

    /** Classify separately so error handling can be tested without opening dialogs. */
    public static ErrorMessage describe(Throwable error) {
        Throwable cause = error;
        while ((cause instanceof CompletionException || cause instanceof ExecutionException)
                && cause.getCause() != null) cause = cause.getCause();
        return switch (cause) {
            case BusinessException business -> new ErrorMessage(Alert.AlertType.WARNING, business.getMessage());
            case ResourceNotFoundException missing -> new ErrorMessage(Alert.AlertType.ERROR, missing.getMessage());
            case CannotCreateTransactionException unavailable -> new ErrorMessage(Alert.AlertType.ERROR,
                    "Không kết nối được cơ sở dữ liệu. Kiểm tra SQL Server.");
            case null -> new ErrorMessage(Alert.AlertType.ERROR, "Lỗi hệ thống, vui lòng thử lại.");
            default -> {
                log.error("Lỗi không mong muốn", cause);
                yield new ErrorMessage(Alert.AlertType.ERROR, "Lỗi hệ thống, vui lòng thử lại.");
            }
        };
    }

    public static void info(String message) { show(Alert.AlertType.INFORMATION, "Thông báo", message); }
    public static void warn(String message) { show(Alert.AlertType.WARNING, "Cảnh báo", message); }
    public static boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        alert.setTitle("Xác nhận");
        alert.setHeaderText(null);
        return alert.showAndWait().filter(ButtonType.YES::equals).isPresent();
    }
    private static void show(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
    public record ErrorMessage(Alert.AlertType type, String text) { }
}
