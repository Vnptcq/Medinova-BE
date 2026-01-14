package com.project.medinova.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

    /**
     * Redirect root path to Swagger UI
     */
    @GetMapping("/")
    public String index() {
        return "redirect:/swagger-ui.html";
    }
}
