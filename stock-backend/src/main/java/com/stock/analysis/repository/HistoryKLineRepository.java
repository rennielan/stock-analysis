package com.stock.analysis.repository;

import com.stock.analysis.entity.HistoryKLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HistoryKLineRepository extends JpaRepository<HistoryKLine, Long> {
    
    /**
     * 获取某只股票指定周期的最新一条K线数据
     */
    @Query(value = "SELECT * FROM history_k_lines WHERE code = :code AND period = :period ORDER BY trade_date DESC LIMIT 1", nativeQuery = true)
    Optional<HistoryKLine> findLatestByCodeAndPeriod(@Param("code") String code, @Param("period") String period);
}
