package com.funews.backend.repository;

import com.funews.backend.entity.NewsArticle;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {
    List<NewsArticle> findAllByOrderByIdAsc();
    List<NewsArticle> findTop5ByOrderByCreatedDateDescIdDesc();
    boolean existsByCategoryId(Long categoryId);
    boolean existsByCreatedById(Long accountId);
    long countByStatus(int status);
}
