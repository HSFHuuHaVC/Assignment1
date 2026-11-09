package com.fu.cinema.desktop.session;

import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.exception.BusinessException;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import org.springframework.stereotype.Component;

/** One authenticated user per desktop process; mutate from the FX thread. */
@Component
public class DesktopSession {
    private volatile SessionUser user;
    private final ReadOnlyStringWrapper displayName = new ReadOnlyStringWrapper("");

    public void login(SessionUser candidate) {
        if (candidate == null || !candidate.isAdmin()) {
            throw new BusinessException("Ứng dụng quầy vé chỉ dành cho nhân viên (tài khoản ADMIN)");
        }
        user = candidate;
        displayName.set(candidate.getDisplayName());
    }

    public void logout() {
        user = null;
        displayName.set("");
    }

    public SessionUser getUser() { return user; }
    public ReadOnlyStringProperty displayNameProperty() { return displayName.getReadOnlyProperty(); }

    /** Call at the beginning of every desktop facade write method. */
    public void requireAdmin() {
        SessionUser current = user;
        if (current == null || !current.isAdmin()) {
            throw new BusinessException("Bạn không có quyền thực hiện thao tác này");
        }
    }
}
