package com.funews.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Danh gia sao - bang article_ratings.
 *
 * Doc gia cham 1-5 sao. Moi nguoi chi cham MOT lan cho MOT bai, nhung duoc sua
 * diem: khi do UPDATE dong cu chu khong INSERT dong moi. Rang buoc mot nguoi
 * mot bai la filtered index ux_ratings_article_account (WHERE is_deleted = 0),
 * khong khai duoc bang JPA nen khong co @UniqueConstraint o day.
 *
 * Cot score la TINYINT voi rang buoc ck_ratings_score (score BETWEEN 1 AND 5).
 *
 * Luu y cho tang service: moi lan them, sua hay xoa mem mot danh gia thi phai
 * cap nhat ratingSum va ratingCount cua NewsArticle trong CUNG mot transaction.
 */
@Entity
@Table(name = "article_ratings")
public class ArticleRating {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    private NewsArticle article;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false)
    private int score;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public ArticleRating() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public NewsArticle getArticle() { return article; }
    public void setArticle(NewsArticle article) { this.article = article; }
    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
