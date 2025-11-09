package com.example.test1.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "stock_data")
public class StockData {

    @Id
    private String id;

    @Indexed
    private LocalDateTime timestamp;

    @Indexed
    private String name;

    private BigDecimal last;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal chg;
    private String chgPercent;
    private String vol;
    private String time;
    private List<BigDecimal> historicalPrices;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getLast() { return last; }
    public void setLast(BigDecimal last) { this.last = last; }

    public BigDecimal getHigh() { return high; }
    public void setHigh(BigDecimal high) { this.high = high; }

    public BigDecimal getLow() { return low; }
    public void setLow(BigDecimal low) { this.low = low; }

    public BigDecimal getChg() { return chg; }
    public void setChg(BigDecimal chg) { this.chg = chg; }

    public String getChgPercent() { return chgPercent; }
    public void setChgPercent(String chgPercent) { this.chgPercent = chgPercent; }

    public String getVol() { return vol; }
    public void setVol(String vol) { this.vol = vol; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public List<BigDecimal> getHistoricalPrices() {
        return historicalPrices;
    }
 
    public void setHistoricalPrices(List<BigDecimal> historicalPrices) {
        this.historicalPrices = historicalPrices;
    }
}