package com.ridivi.ridivipay.admin.controllers;

import com.ridivi.ridivipay.admin.services.VersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MainController {
  private final VersionService versionService;

  @Autowired
  public MainController(VersionService versionService) {
    this.versionService = versionService;
  }

  @RequestMapping("/version")
  public String getVersion() {
    return versionService.getVersion();
  }
}