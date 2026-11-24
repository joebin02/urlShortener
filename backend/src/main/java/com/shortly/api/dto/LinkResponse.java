package com.shortly.api.dto;
import com.shortly.domain.ShortLink;
import java.time.Instant;
public record LinkResponse(String code, String originalUrl, String shortUrl, long clicks, Instant createdAt) {
    public static LinkResponse from(ShortLink link, String baseUrl) { return new LinkResponse(link.getShortCode(), link.getOriginalUrl(), baseUrl + "/" + link.getShortCode(), link.getClickCount(), link.getCreatedAt()); }
}
