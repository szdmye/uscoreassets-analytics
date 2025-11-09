package com.example.test1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.example.test1")
@EntityScan("com.example.test1.model")
public class Test1Application {
    public static void main(String[] args) {
        SpringApplication.run(Test1Application.class, args);
    }
}