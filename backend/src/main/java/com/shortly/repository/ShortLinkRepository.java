package com.shortly.repository;
import com.shortly.domain.ShortLink;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ShortLinkRepository extends JpaRepository<ShortLink, Long> {
    Optional<ShortLink> findByShortCode(String shortCode);
    boolean existsByShortCode(String shortCode);
    List<ShortLink> findTop100ByOrderByCreatedAtDesc();
}
