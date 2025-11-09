package com.example.test1.service;

import com.example.test1.model.StockData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface StockDataService {
    Page<StockData> getAllStockData(Pageable pageable);

    List<StockData> searchByName(String name);

    List<StockData> filterByTimeRange(LocalDateTime startTime, LocalDateTime endTime);

    List<StockData> searchByNameAndTimeRange(String name, LocalDateTime startTime, LocalDateTime endTime);

    StockData saveStockData(StockData stockData);
}