package com.shortly.repository;
import com.shortly.domain.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {
    long countByShortCode(String shortCode);
    @Query("select function('date_trunc', 'day', c.clickedAt), count(c) from ClickEvent c where c.shortCode = :code and c.clickedAt >= :since group by function('date_trunc', 'day', c.clickedAt) order by function('date_trunc', 'day', c.clickedAt)")
    List<Object[]> dailyCounts(@Param("code") String code, @Param("since") Instant since);
}
