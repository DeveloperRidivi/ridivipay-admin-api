package com.ridivi.ridivipay.admin.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cuentas")
public class CuentasController {
  public CuentasController() {
  }

  @RequestMapping("/info")
  public String getVersion() {
    return "Cuentas INFO";
  }
}