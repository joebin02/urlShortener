package com.shortly.service;

import com.shortly.domain.ShortLink;
import com.shortly.repository.ShortLinkRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.util.Optional;

@Service
public class ShortLinkService {
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final SecureRandom random = new SecureRandom();
    private final ShortLinkRepository repository;
    private final StringRedisTemplate redis;
    private final String baseUrl;
    public ShortLinkService(ShortLinkRepository repository, StringRedisTemplate redis, @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.repository = repository; this.redis = redis; this.baseUrl = baseUrl;
    }
    @Transactional
    public ShortLink create(String url) {
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = nextCode();
            if (!repository.existsByShortCode(code)) {
                try { return repository.saveAndFlush(new ShortLink(code, url)); }
                catch (org.springframework.dao.DataIntegrityViolationException collision) { /* retry unique-key race */ }
            }
        }
        throw new IllegalStateException("Could not allocate a unique short code");
    }
    public java.util.List<ShortLink> list() { return repository.findTop100ByOrderByCreatedAtDesc(); }
    public Optional<String> resolve(String code) {
        String cached = redis.opsForValue().get("url:" + code);
        if (cached != null) return Optional.of(cached);
        return repository.findByShortCode(code).map(link -> {
            redis.opsForValue().set("url:" + code, link.getOriginalUrl(), java.time.Duration.ofHours(24));
            return link.getOriginalUrl();
        });
    }
    public String shortUrl(String code) { return baseUrl + "/" + code; }
    private String nextCode() {
        var result = new StringBuilder(7);
        for (int i = 0; i < 7; i++) result.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        return result.toString();
    }
}
