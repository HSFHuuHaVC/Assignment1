package com.fu.cinema.desktop.util;

import jakarta.validation.Validator;
import javafx.css.PseudoClass;
import javafx.scene.control.Control;
import javafx.scene.control.Tooltip;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class FxValidator {
    private static final PseudoClass ERROR = PseudoClass.getPseudoClass("error");
    private final Validator validator;

    public Map<String, String> validate(Object form) {
        Map<String, String> errors = new LinkedHashMap<>();
        validator.validate(form).stream()
                .sorted(java.util.Comparator.comparing(v -> v.getPropertyPath().toString() + ":" + v.getMessage()))
                .forEach(v -> errors.putIfAbsent(v.getPropertyPath().toString(), v.getMessage()));
        return errors;
    }

    public boolean mark(Map<String, String> errors, Map<String, Control> fields) {
        fields.forEach((name, control) -> {
            String message = errors.get(name);
            control.pseudoClassStateChanged(ERROR, message != null);
            control.setTooltip(message == null ? null : new Tooltip(message));
        });
        return errors.isEmpty();
    }
}
