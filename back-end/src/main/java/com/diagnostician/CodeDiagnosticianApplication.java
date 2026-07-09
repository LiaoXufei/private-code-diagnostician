package com.diagnostician;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

//@SpringBootApplication(exclude= {DataSourceAutoConfiguration.class})
@SpringBootApplication(scanBasePackages = {"com.diagnostician"})
public class CodeDiagnosticianApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeDiagnosticianApplication.class, args);
    }

}