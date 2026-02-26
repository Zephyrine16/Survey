package com.example.survey;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api")
public class HelloController {
    @GetMapping("/message")
    public String getMessage() {
        return "Hello from Spring Boot!";
    }
}
