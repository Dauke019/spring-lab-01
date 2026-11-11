package kz.iitu.springlab.web;

import java.util.Comparator;

import kz.iitu.springlab.security.AppUser;
import kz.iitu.springlab.security.AppUserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Task 3.1: the administrator section. Access is decided by the URL rule /admin/** -> hasRole("ADMIN"). */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AppUserRepository users;

    public AdminController(AppUserRepository users) {
        this.users = users;
    }

    @GetMapping
    public String users(Model model) {
        model.addAttribute("users", users.findAll().stream()
                .sorted(Comparator.comparing(AppUser::getId)).toList());
        return "admin/users";
    }
}
