package com.fu.cinema.exception;

import com.fu.cinema.util.SessionUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.net.URI;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler({ResourceNotFoundException.class, NoResourceFoundException.class,
            MethodArgumentTypeMismatchException.class})
    public ModelAndView handleNotFound(Exception ex, HttpServletRequest request) {
        log.debug("Not found: {}", ex.getMessage());
        return errorPage(HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView handleForbidden(AccessDeniedException ex, HttpServletRequest request) {
        return errorPage(HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(BusinessException.class)
    public String handleBusiness(BusinessException ex, HttpServletRequest request) {
        RequestContextUtils.getOutputFlashMap(request).put("error", ex.getMessage());
        return "redirect:" + previousLocalPage(request);
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleOther(Exception ex, HttpServletRequest request) {
        // Preserve MVC statuses such as 400 and 405 without exposing exception details.
        if (ex instanceof ErrorResponse response) {
            return errorPage(response.getStatusCode(), request);
        }
        log.error("Unexpected error", ex);
        return errorPage(HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    private ModelAndView errorPage(HttpStatusCode status, HttpServletRequest request) {
        String view = switch (status.value()) {
            case 403 -> "error/403";
            case 404 -> "error/404";
            case 500 -> "error/500";
            default -> "error";
        };
        ModelAndView result = new ModelAndView(view);
        result.setStatus(status);
        result.addObject("status", status.value());
        // Exception rendering uses a new model; restore the shared session menu.
        result.addObject("currentUser", SessionUtils.getCurrentUser(request.getSession(false)));
        return result;
    }

    private String previousLocalPage(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return "/";
        }
        try {
            URI uri = URI.create(referer);
            if (uri.isOpaque() || uri.getRawUserInfo() != null) {
                return "/";
            }
            if (uri.isAbsolute()) {
                boolean http = "http".equalsIgnoreCase(uri.getScheme())
                        || "https".equalsIgnoreCase(uri.getScheme());
                int port = uri.getPort() >= 0 ? uri.getPort()
                        : ("https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80);
                if (!http || !request.getScheme().equalsIgnoreCase(uri.getScheme())
                        || !request.getServerName().equalsIgnoreCase(uri.getHost())
                        || request.getServerPort() != port) {
                    return "/";
                }
            } else if (uri.getRawAuthority() != null) {
                return "/";
            }
            String path = uri.getRawPath();
            if (path == null || path.isEmpty()) {
                path = "/";
            }
            if (!path.startsWith("/") || path.startsWith("//") || path.contains("\\")) {
                return "/";
            }
            return path + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
        } catch (IllegalArgumentException ex) {
            return "/";
        }
    }
}
