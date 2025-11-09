package com.example.test1;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontendController {
    @GetMapping(value = {"/", "/{path:[^.]*}"})
    public String index() {
        return "forward:/index.html";
    }
}