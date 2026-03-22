package com.sahil.repository;

import com.sahil.model.UrlAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UrlAnalyticsRepository extends JpaRepository<UrlAnalytics, Long> {
    List<UrlAnalytics> findByShortCode(String shortCode);
    Long countByShortCode(String shortCode);
}
