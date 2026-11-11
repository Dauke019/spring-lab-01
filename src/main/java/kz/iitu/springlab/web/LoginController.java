package kz.iitu.springlab.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Variant 1: our own login page. Only GET is ours - POST /login is still handled by
 * UsernamePasswordAuthenticationFilter, which redirects to /login?error or to the saved request.
 */
@Controller
public class LoginController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
