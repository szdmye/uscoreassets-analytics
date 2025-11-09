package com.example.test1.service;

// import com.example.test1.model.StockData;
import com.example.test1.repository.StockDataRepository;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class CsvImportService {

    @Autowired
    private StockDataRepository stockDataRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    public void importCsvData(String filePath) {
        System.out.println("开始导入CSV文件: " + filePath);
        
        try (Reader reader = new FileReader(filePath);
             // 使用更宽松的CSV格式，处理不一致的分隔符
             CSVParser csvParser = CSVParser.parse(reader, CSVFormat.DEFAULT
                 .withFirstRecordAsHeader()
                 .withIgnoreSurroundingSpaces()
                 .withTrim())) {

            System.out.println("CSV文件头: " + csvParser.getHeaderMap().keySet());
            
            List<Document> docs = new ArrayList<>();
            int recordCount = 0;
            int errorCount = 0;
            
            for (CSVRecord record : csvParser) {
                recordCount++;
                
                try {
                    // 调试：打印有问题的记录
                    if (recordCount <= 5) {
                        System.out.println("记录 " + recordCount + ": " + record.toString());
                    }

                    // 构建 BSON Document 并尽量保留 CSV 的列名
                    // 为兼容不同 CSV 的列名变体，先建立一个简易的列名归一化表
                    java.util.Map<String, String> normalize = new java.util.HashMap<>();
                    normalize.put("chg_", "chg");
                    normalize.put("chg_%", "chgPercent");
                    normalize.put("chg%", "chgPercent");
                    normalize.put("chg_chg_%", "chgPercent");
                    normalize.put("chg", "chg");
                    normalize.put("chgpercent", "chgPercent");
                    normalize.put("vol_", "vol");
                    normalize.put("vol", "vol");

                    Document doc = new Document();
                    for (String header : csvParser.getHeaderNames()) {
                        String headerKey = header == null ? "" : header.trim();
                        String headerLower = headerKey.toLowerCase();
                        String normalized = normalize.getOrDefault(headerLower, headerKey);
                        String raw = null;
                        try {
                            raw = record.get(header);
                        } catch (IllegalArgumentException e) {
                            raw = null;
                        }

                        if (raw == null) {
                            doc.put(normalized, null);
                            continue;
                        }

                        String value = raw.trim();
                        if (value.isEmpty()) {
                            doc.put(header, null);
                            continue;
                        }

                        if (headerLower.equals("timestamp")) {
                            String cleaned = value.replace('@', '0').replaceAll("[^0-9\\-:/\\s]", "");
                            cleaned = cleanTimestamp(cleaned);
                            LocalDateTime ldt = parseTimestamp(cleaned);
                            java.time.Instant instant = ldt.atZone(java.time.ZoneId.systemDefault()).toInstant();
                            String iso = java.time.format.DateTimeFormatter.ISO_INSTANT.format(instant);
                            doc.put(normalized, java.util.Date.from(instant));
                            doc.put(normalized + "_iso", iso);
                            continue;
                        }

                        if (value.matches("^-?\\d+$")) {
                            try {
                                long lv = Long.parseLong(value);
                                doc.put(normalized, lv);
                                continue;
                            } catch (NumberFormatException ignored) {
                                // fallthrough
                            }
                        }

                        if (value.matches("^-?\\d+\\.\\d+$")) {
                            try {
                                double dv = Double.parseDouble(value);
                                doc.put(normalized, dv);
                                continue;
                            } catch (NumberFormatException ignored) {
                                // fallthrough
                            }
                        }

                        doc.put(normalized, value);
                    }

                    docs.add(doc);

                    // 打印前5个将要插入的 Document
                    if (recordCount <= 5) {
                        try {
                            System.out.println("[DEBUG] 将插入的 Document 示例: " + doc.toJson());
                        } catch (Exception ex) {
                            System.out.println("[DEBUG] Document toJson 打印失败，使用 toString: " + doc.toString());
                        }
                    }

                    // 分批插入到 MongoDB
                    if (docs.size() >= 1000) {
                        for (Document d : docs) {
                            mongoTemplate.insert(d, "stock_data");
                        }
                        System.out.println("已导入 " + recordCount + " 条记录...");
                        docs.clear();
                    }

                } catch (Exception e) {
                    errorCount++;
                    System.out.println("解析记录 " + recordCount + " 时出错: " + e.getMessage());
                    System.out.println("问题记录内容: " + record.toString());
                    // 继续处理下一条记录，不中断整个导入过程
                    continue;
                }
            }
            
            // 保存剩余数据
            if (!docs.isEmpty()) {
                for (Document d : docs) {
                    mongoTemplate.insert(d, "stock_data");
                }
            }
            
            System.out.println("CSV数据导入完成！总共处理记录数: " + recordCount + ", 错误记录数: " + errorCount);
            
        } catch (Exception e) {
            System.err.println("CSV数据导入失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("CSV数据导入失败: " + e.getMessage(), e);
        }
    }
    
    // 清理时间字符串
    private String cleanTimestamp(String timestamp) {
        if (timestamp == null || timestamp.trim().isEmpty()) {
            return "2025-01-01 00:00:00"; // 默认时间
        }
        
        // 替换非法字符
        timestamp = timestamp.replace('@', '0')
                           .replace('＠', '0') // 全角@
                           .replaceAll("[^0-9\\-:\\s]", "");
        
        // 修复无效的小时数（大于23）
        String[] parts = timestamp.split(" ");
        if (parts.length >= 2) {
            String timePart = parts[1];
            String[] timeParts = timePart.split(":");
            if (timeParts.length >= 1) {
                try {
                    int hour = Integer.parseInt(timeParts[0]);
                    if (hour > 23) {
                        timeParts[0] = "23"; // 将无效小时改为23
                    }
                    timePart = String.join(":", timeParts);
                    parts[1] = timePart;
                    timestamp = parts[0] + " " + parts[1];
                } catch (NumberFormatException e) {
                    // 忽略解析错误
                }
            }
        }
        
        return timestamp;
    }
    
    // 解析时间
    private LocalDateTime parseTimestamp(String timestampStr) {
        try {
            // 尝试多种时间格式
            DateTimeFormatter[] formatters = {
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-M-d H:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
            };
            
            for (DateTimeFormatter formatter : formatters) {
                try {
                    return LocalDateTime.parse(timestampStr, formatter);
                } catch (Exception e) {
                    // 尝试下一种格式
                }
            }
            
            // 如果所有格式都失败，使用默认时间
            System.out.println("无法解析时间: " + timestampStr + ", 使用默认时间");
            return LocalDateTime.now();
            
        } catch (Exception e) {
            System.out.println("时间解析失败: " + timestampStr + ", 使用默认时间");
            return LocalDateTime.now();
        }
    }
}