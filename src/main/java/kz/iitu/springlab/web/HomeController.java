package kz.iitu.springlab.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Lab 10: the start page is a template now, so it can show who is looking at it. */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "index";
    }
}
