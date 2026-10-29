package com.funews.backend.repository;

import com.funews.backend.entity.ArticleRating;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRatingRepository extends JpaRepository<ArticleRating, Long> {
    /**
     * Moi nguoi chi co MOT diem cho MOT bai. Rang buoc that la filtered unique
     * index ux_ratings_article_account (WHERE is_deleted = 0) duoi database;
     * truy van nay de tang service tim dong cu ma sua thay vi them dong moi.
     */
    Optional<ArticleRating> findByArticleIdAndAccountIdAndDeletedFalse(Long articleId, Long accountId);
}
