package com.fsmonitor.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaController {
    
    // Forward all non-API routes to index.html for React Router
    @RequestMapping(value = {"/", "/login", "/dashboard", "/settings"})
    public String forward() {
        return "forward:/index.html";
    }
}
