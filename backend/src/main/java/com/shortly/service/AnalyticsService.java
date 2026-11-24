package com.shortly.service;

import com.shortly.domain.ClickEvent;
import com.shortly.domain.ShortLink;
import com.shortly.repository.ClickEventRepository;
import com.shortly.repository.ShortLinkRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AnalyticsService {
    private final ClickEventRepository clicks;
    private final ShortLinkRepository links;
    public AnalyticsService(ClickEventRepository clicks, ShortLinkRepository links) { this.clicks = clicks; this.links = links; }
    @Async
    @Transactional
    public void recordClick(String code) {
        clicks.save(new ClickEvent(code));
        links.findByShortCode(code).ifPresent(link -> { link.incrementClicks(); links.save(link); });
    }
    @Transactional(readOnly = true)
    public Map<String, Object> getStats(String code) {
        ShortLink link = links.findByShortCode(code).orElseThrow(() -> new IllegalArgumentException("Unknown short code"));
        var daily = new LinkedHashMap<String, Long>();
        for (Object[] row : clicks.dailyCounts(code, Instant.now().minus(7, ChronoUnit.DAYS)))
            daily.put(((java.sql.Timestamp) row[0]).toLocalDateTime().toLocalDate().toString(), ((Number) row[1]).longValue());
        return Map.of("code", code, "originalUrl", link.getOriginalUrl(), "totalClicks", clicks.countByShortCode(code), "last7Days", daily);
    }
}
