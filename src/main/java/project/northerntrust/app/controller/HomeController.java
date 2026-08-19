package project.northerntrust.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping({"/", "/dashboard", "/login"})
    public String home() {
        return "redirect:/usa/login.html";
    }
}
