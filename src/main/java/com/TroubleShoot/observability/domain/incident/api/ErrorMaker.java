package com.troubleshoot.observability.domain.incident.api;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/boom")
public class ErrorMaker {

    @GetMapping
    public String boom() {

        throw new RuntimeException("test error");
    }
}
