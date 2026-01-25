package com.stock.analysis.repository;

import com.stock.analysis.entity.DailyKLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DailyKLineRepository extends JpaRepository<DailyKLine, Long> {
    
    /**
     * 获取某只股票最新的一条日线数据 (使用 code)
     */
    @Query(value = "SELECT * FROM daily_k_lines WHERE code = :code ORDER BY trade_date DESC LIMIT 1", nativeQuery = true)
    Optional<DailyKLine> findLatestByCode(@Param("code") String code);
}