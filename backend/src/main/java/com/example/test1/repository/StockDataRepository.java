package com.example.test1.repository;

import com.example.test1.model.StockData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockDataRepository extends MongoRepository<StockData, String> {
    
    // 保持与JPA相同的方法名称
    List<StockData> findByName(String name);

    List<StockData> findByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime);

    // 使用MongoDB的查询语法实现模糊搜索
    @Query("{ 'name': { $regex: ?0, $options: 'i' }, 'timestamp': { $gte: ?1, $lte: ?2 } }")
    List<StockData> findByNameContainingAndTimestampBetween(String name, LocalDateTime startTime, LocalDateTime endTime);

    // 分页查询 - 方法名称与JPA保持一致
    Page<StockData> findAll(Pageable pageable);
}