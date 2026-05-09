package com.stock.analysis.repository;

import com.stock.analysis.entity.TradeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradeRecordRepository extends JpaRepository<TradeRecord, String> {
    
    /**
     * 按交易时间倒序查询所有记录
     */
    List<TradeRecord> findAllByOrderByTradeTimeDesc();
}
