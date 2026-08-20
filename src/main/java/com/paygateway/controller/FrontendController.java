package com.paygateway.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontendController {

    @GetMapping({
            "/",
            "/login",
            "/register",
            "/payments",
            "/payments/{id}",
            "/refunds",
            "/webhooks",
            "/api-keys"
    })
    public String dashboard() {
        return "forward:/index.html";
    }
}