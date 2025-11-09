package com.example.test1.config;

import com.example.test1.service.CsvImportService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class DataInitializer implements CommandLineRunner {

    private final CsvImportService csvImportService;

    // 使用构造函数注入
    public DataInitializer(CsvImportService csvImportService) {
        this.csvImportService = csvImportService;
    }

    @Override
    public void run(String... args) throws Exception {
        // 尝试多个可能的文件位置
        String[] possiblePaths = {
            "stocks.csv",                                   // 项目根目录
            "src/main/resources/stocks.csv",               // resources目录
            "data/stocks.csv",                             // data目录
            System.getProperty("user.dir") + "/stocks.csv" // 绝对路径
        };

        boolean fileFound = false;
        
        for (String filePath : possiblePaths) {
            File file = new File(filePath);
            if (file.exists() && !file.isDirectory()) {
                try {
                    System.out.println("找到CSV文件: " + file.getAbsolutePath());
                    System.out.println("文件大小: " + file.length() + " bytes");
                    csvImportService.importCsvData(filePath);
                    fileFound = true;
                    break; // 导入成功则退出循环
                } catch (Exception e) {
                    System.out.println("从 " + filePath + " 导入失败: " + e.getMessage());
                    e.printStackTrace(); // 打印完整堆栈跟踪
                }
            } else {
                System.out.println("文件不存在: " + file.getAbsolutePath());
            }
        }
        
        if (!fileFound) {
            System.out.println("未找到stocks.csv文件，请确保文件存在于以下位置之一:");
            for (String path : possiblePaths) {
                System.out.println(" - " + new File(path).getAbsolutePath());
            }
            System.out.println("当前工作目录: " + System.getProperty("user.dir"));
        }
    }
}