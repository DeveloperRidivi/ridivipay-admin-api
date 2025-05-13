package com.ridivi.ridivipay.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
@Configuration

public class RidiviAdminApiApplication {
  public static void main(String[] args) {
    SpringApplication.run(RidiviAdminApiApplication.class, args);
  }
}
