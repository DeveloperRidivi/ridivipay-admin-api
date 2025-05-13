package com.ridivi.ridivipay.admin.services;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("local")
public class VersionServiceLocalImpl implements VersionService {
  @Override
  public String getVersion() {
    return "LOCAL - v.1.0.0";
  }
}
