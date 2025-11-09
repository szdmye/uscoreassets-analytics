package com.example.test1.service.impl;

import com.example.test1.model.StockData;
import com.example.test1.repository.StockDataRepository;
import com.example.test1.service.StockDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StockDataServiceImpl implements StockDataService {
    
    @Autowired
    private StockDataRepository stockDataRepository;

    @Override
    public Page<StockData> getAllStockData(Pageable pageable) {
        return stockDataRepository.findAll(pageable);
    }

    @Override
    public List<StockData> searchByName(String name) {
        return stockDataRepository.findByName(name);
    }

    @Override
    public List<StockData> filterByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return stockDataRepository.findByTimestampBetween(startTime, endTime);
    }

    @Override
    public List<StockData> searchByNameAndTimeRange(String name, LocalDateTime startTime, LocalDateTime endTime) {
        return stockDataRepository.findByNameContainingAndTimestampBetween(name, startTime, endTime);
    }

    @Override
    public StockData saveStockData(StockData stockData) {
        return stockDataRepository.save(stockData);
    }
}