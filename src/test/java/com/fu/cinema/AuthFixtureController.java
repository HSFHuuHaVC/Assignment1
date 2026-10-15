package com.fu.cinema;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Test-only destinations until the independently assigned feature pages exist. */
@Controller
@Profile("auth-fixture")
public class AuthFixtureController {
    @GetMapping({"/", "/admin", "/admin/movies", "/customer/profile"})
    public String destination() {
        return "auth-fixture";
    }
}
