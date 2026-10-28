package com.funews.backend.repository;

import com.funews.backend.entity.ArticleStatus;
import com.funews.backend.entity.NewsArticle;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Moi truy van o day deu loc deleted = false.
 *
 * Khong co phuong thuc nao tra ve ca bai da xoa mem: xoa mem ma van tra ra thi
 * bang xoa that, chi khac cai ten.
 */
public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {

    /** EDITOR va ADMIN thay het moi trang thai. */
    @Query("select a from NewsArticle a where a.deleted = false order by a.id asc")
    List<NewsArticle> findAllVisible();

    /** Khach va READER: chi bai da xuat ban. */
    @Query("""
            select a from NewsArticle a
            where a.deleted = false and a.status = :published
            order by a.publishedAt desc, a.id desc
            """)
    List<NewsArticle> findPublished(@Param("published") ArticleStatus published);

    /** CONTRIBUTOR: bai da xuat ban cua moi nguoi, cong bai cua chinh minh o moi trang thai. */
    @Query("""
            select a from NewsArticle a
            where a.deleted = false and (a.status = :published or a.author.id = :authorId)
            order by a.id asc
            """)
    List<NewsArticle> findVisibleForAuthor(@Param("published") ArticleStatus published,
                                           @Param("authorId") Long authorId);

    /** Hang doi duyet bai: nop truoc duyet truoc. */
    @Query("""
            select a from NewsArticle a
            where a.deleted = false and a.status = :status
            order by a.submittedAt asc, a.id asc
            """)
    List<NewsArticle> findByStatusForQueue(@Param("status") ArticleStatus status);

    /** Tac vu nen: bai da duyet va da den gio hen - FR-ED-003. */
    @Query("""
            select a from NewsArticle a
            where a.deleted = false and a.status = :scheduled and a.scheduledPublishAt <= :now
            order by a.scheduledPublishAt asc
            """)
    List<NewsArticle> findDueForPublish(@Param("scheduled") ArticleStatus scheduled,
                                        @Param("now") LocalDateTime now);

    List<NewsArticle> findTop5ByDeletedFalseOrderByCreatedAtDescIdDesc();

    Optional<NewsArticle> findBySlugAndDeletedFalse(String slug);

    boolean existsByCategoryIdAndDeletedFalse(Long categoryId);

    long countByDeletedFalse();

    long countByStatusAndDeletedFalse(ArticleStatus status);
}
