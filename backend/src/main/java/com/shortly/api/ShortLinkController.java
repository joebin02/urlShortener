package com.shortly.api;

import com.shortly.api.dto.CreateLinkRequest;
import com.shortly.api.dto.LinkResponse;
import com.shortly.service.AnalyticsService;
import com.shortly.service.RateLimitService;
import com.shortly.service.ShortLinkService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.Map;

@RestController
public class ShortLinkController {
    private final ShortLinkService links;
    private final AnalyticsService analytics;
    private final RateLimitService limits;
    public ShortLinkController(ShortLinkService links, AnalyticsService analytics, RateLimitService limits) { this.links = links; this.analytics = analytics; this.limits = limits; }

    @GetMapping("/api/links")
    public java.util.List<LinkResponse> list() { return links.list().stream().map(link -> LinkResponse.from(link, links.shortUrl("").replaceAll("/$", ""))).toList(); }

    @PostMapping("/api/links")
    public ResponseEntity<?> create(@Valid @RequestBody CreateLinkRequest request, HttpServletRequest http) {
        String ip = clientIp(http);
        if (!limits.allow("create:" + ip)) return ResponseEntity.status(429).body(Map.of("error", "Rate limit exceeded. Try again shortly."));
        var link = links.create(request.url());
        return ResponseEntity.status(HttpStatus.CREATED).body(LinkResponse.from(link, links.shortUrl("").replaceAll("/$", "")));
    }
    @GetMapping("/api/links/{code}/stats")
    public Map<String, Object> stats(@PathVariable String code) { return analytics.getStats(code); }
    @GetMapping("/{code:[a-zA-Z0-9]{7}}")
    public ResponseEntity<Void> redirect(@PathVariable String code, HttpServletRequest http) {
        String ip = clientIp(http);
        if (!limits.allow("redirect:" + ip)) return ResponseEntity.status(429).build();
        return links.resolve(code).map(url -> {
            analytics.recordClick(code);
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).<Void>build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
    private String clientIp(HttpServletRequest request) {
        // Trust forwarded addresses only when the service is behind a trusted proxy configured by the operator.
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null || forwarded.isBlank() ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
    }
}
