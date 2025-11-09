package com.example.test1.controller;

import com.example.test1.model.StockData;
import com.example.test1.service.StockDataService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/stock-data")
public class StockDataController {
    @Autowired
    private StockDataService stockDataService;


    // 1. 分页查询所有数据
    @GetMapping
    public ResponseEntity<Page<StockData>> getAllStockData(
            @PageableDefault(size = 10, sort = "timestamp") Pageable pageable) {
        return ResponseEntity.ok(stockDataService.getAllStockData(pageable));
    }

    // 2. 按名称查询
    @GetMapping("/search-by-name")
    public ResponseEntity<List<StockData>> searchByName(@RequestParam String name) {
        return ResponseEntity.ok(stockDataService.searchByName(name));
    }

    // 3. 按时间范围查询
    @GetMapping("/search-by-time")
    public ResponseEntity<List<StockData>> searchByTime(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ResponseEntity.ok(stockDataService.filterByTimeRange(startTime, endTime));
    }

    // 4. 同时按名称和时间范围查询
    @GetMapping("/advanced-search")
    public ResponseEntity<List<java.util.Map<String, Object>>> advancedSearch(
            @RequestParam String name,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        List<StockData> results = stockDataService.searchByNameAndTimeRange(name, startTime, endTime);

        java.util.List<java.util.Map<String, Object>> mapped = results.stream().map(sd -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", sd.getId());
            map.put("name", sd.getName());
            map.put("last", sd.getLast() != null ? sd.getLast().toString() : null);
            map.put("high", sd.getHigh() != null ? sd.getHigh().toString() : null);
            map.put("low", sd.getLow() != null ? sd.getLow().toString() : null);
            map.put("chg", sd.getChg() != null ? sd.getChg().toString() : null);
            map.put("chgPercent", sd.getChgPercent());
            map.put("vol", sd.getVol());
            map.put("time", sd.getTime());
            map.put("historicalPrices", sd.getHistoricalPrices());

            // timestamp -> ISO 8601 string for front-end new Date(...) 兼容
            if (sd.getTimestamp() != null) {
                java.time.LocalDateTime ldt = sd.getTimestamp();
                java.time.Instant instant = ldt.atZone(java.time.ZoneId.systemDefault()).toInstant();
                String iso = java.time.format.DateTimeFormatter.ISO_INSTANT.format(instant);
                map.put("timestamp", iso);
            } else {
                map.put("timestamp", null);
            }

            return map;
        }).toList();

        return ResponseEntity.ok(mapped);
    }

    // 5. 保存数据
    @PostMapping
    public ResponseEntity<StockData> saveStockData(@Valid @RequestBody StockData stockData) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stockDataService.saveStockData(stockData));
    }
}