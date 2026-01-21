package com.stock.analysis.config;

import com.stock.analysis.entity.Stock;
import com.stock.analysis.entity.StrategyType;
import com.stock.analysis.repository.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataInitializer implements CommandLineRunner {
    
    @Autowired
    private StockRepository stockRepository;
    
    @Override
    public void run(String... args) throws Exception {
        // 如果数据库为空，插入一些测试数据
        if (stockRepository.count() == 0) {
            Stock stock1 = new Stock("AAPL", new BigDecimal("150.25"), new BigDecimal("2.5"));
            stock1.setStrategy(StrategyType.WATCH);
            stock1.setTargetPrice(new BigDecimal("160.00"));
            stock1.setStopLoss(new BigDecimal("145.00"));
            stock1.setConfidence(4);
            stock1.setNotes("苹果公司，关注其新产品发布");
            
            Stock stock2 = new Stock("GOOGL", new BigDecimal("2800.50"), new BigDecimal("-1.2"));
            stock2.setStrategy(StrategyType.BUY_READY);
            stock2.setTargetPrice(new BigDecimal("3000.00"));
            stock2.setStopLoss(new BigDecimal("2700.00"));
            stock2.setConfidence(5);
            stock2.setNotes("谷歌母公司，AI技术领先");
            
            Stock stock3 = new Stock("TSLA", new BigDecimal("800.75"), new BigDecimal("5.8"));
            stock3.setStrategy(StrategyType.HOLDING);
            stock3.setTargetPrice(new BigDecimal("900.00"));
            stock3.setStopLoss(new BigDecimal("750.00"));
            stock3.setConfidence(3);
            stock3.setNotes("特斯拉，电动汽车龙头");
            
            stockRepository.save(stock1);
            stockRepository.save(stock2);
            stockRepository.save(stock3);
            
            System.out.println("测试数据初始化完成，插入了3条股票记录");
        }
    }
}