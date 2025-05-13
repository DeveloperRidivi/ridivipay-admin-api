package com.ridivi.ridivipay.admin.services;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile({"dev", "qa"})
public class VersionServiceDevImpl implements VersionService {
  @Override
  public String getVersion() {
    return "DEV - v.1.0.0";
  }
}
