package com.example.test1.controller;

import com.example.test1.repository.StockDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private StockDataRepository stockDataRepository;

    @GetMapping("/db-connection")
    public String testDbConnection() {
        try {
            // 尝试查询数据
            long count = stockDataRepository.count();
            return "MySQL connection successful! Total records: " + count;
        } catch (Exception e) {
            return "MySQL connection failed: " + e.getMessage();
        }
    }
}