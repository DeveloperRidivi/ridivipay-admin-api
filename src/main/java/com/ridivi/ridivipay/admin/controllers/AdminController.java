package com.ridivi.ridivipay.admin.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
  public AdminController() {
  }

  @RequestMapping("/info")
  public String getVersion() {
    return "Admin INFO";
  }
}