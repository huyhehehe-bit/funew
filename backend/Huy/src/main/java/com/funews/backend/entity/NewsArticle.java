package com.funews.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.generator.EventType;

/**
 * Bai viet - bang articles.
 *
 * Ten class van la NewsArticle (bang truoc day la news_articles) de repository
 * va service hien co khong phai doi ten theo.
 *
 * Nhung thay doi dang chu y so voi ban cu:
 *   headline      -> summary
 *   tags (chuoi)  -> quan he nhieu-nhieu voi Tag qua bang article_tags
 *   status (0/1)  -> ArticleStatus, luu dang chuoi, 6 trang thai
 *   createdBy     -> author (cot author_id)
 *   createdDate   -> createdAt (DATETIME2 thay vi DATE)
 *   them slug, thumbnailUrl, reviewer, cac moc thoi gian cua luong duyet bai,
 *   bo dem luot xem va diem danh gia, co is_featured va xoa mem.
 *
 * ratingAvg la COT TINH TOAN PERSISTED trong database: SQL Server tu tinh tu
 * ratingSum va ratingCount. @Generated bao Hibernate dung bao gio ghi vao cot
 * nay, va doc lai sau moi INSERT hoac UPDATE - nho the diem trung binh khong
 * the lech voi du lieu that.
 */
@Entity
@Table(name = "articles")
public class NewsArticle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 300)
    private String title;

    /** Dang khong dau, dung cho duong dan: "thong-bao-tuyen-sinh-2026". */
    @Column(nullable = false, length = 350)
    private String slug;

    @Column(nullable = false, length = 1000)
    private String summary = "";

    @Lob
    @Column(nullable = false)
    private String content = "";

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(nullable = false, length = 500)
    private String source = "";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ArticleStatus status = ArticleStatus.DRAFT;

    /** Tac gia. Khong doi sau khi tao bai. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private Account author;

    /** Bien tap vien xu ly lan gan nhat. Null khi bai chua nop. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private Account reviewer;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    /** Bat buoc co khi status = SCHEDULED - rang buoc ck_articles_scheduled. */
    @Column(name = "scheduled_publish_at")
    private LocalDateTime scheduledPublishAt;

    /** Bat buoc co khi status = PUBLISHED - rang buoc ck_articles_published. */
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "view_count", nullable = false)
    private long viewCount = 0;

    @Column(name = "rating_count", nullable = false)
    private long ratingCount = 0;

    @Column(name = "rating_sum", nullable = false)
    private long ratingSum = 0;

    /** Chi doc. Database tinh, tang Java khong bao gio ghi. */
    @Generated(event = { EventType.INSERT, EventType.UPDATE })
    @Column(name = "rating_avg", precision = 3, scale = 2)
    private BigDecimal ratingAvg;

    @Column(name = "is_featured", nullable = false)
    private boolean featured = false;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by_id")
    private Account deletedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /* Optimistic locking - cot articles.version. Hibernate tu tang moi lan UPDATE
       va them "AND version = <gia tri doc duoc>" vao WHERE, nen hai nguoi cung sua
       mot bai thi nguoi ghi sau nhan OptimisticLockException chu khong ghi de am
       tham len thay doi cua nguoi truoc. Quan trong nhat la truong hop tac vu hen
       gio dang bai chay cung luc voi bien tap vien bam tay.

       CO Y KHONG CO SETTER: chi Hibernate duoc doi gia tri nay. Muon chan tranh
       chap o tang API thi service so sanh NewsRequest.version voi getVersion()
       roi nem 409, chu dung tu ghi vao day. */
    @Version
    @Column(nullable = false)
    private long version;

    /* Bang noi article_tags chi co hai cot khoa ngoai nen khong can entity rieng.
       LinkedHashSet de thu tu the on dinh khi tra ra JSON. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "article_tags",
            joinColumns = @JoinColumn(name = "article_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new LinkedHashSet<>();

    public NewsArticle() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public ArticleStatus getStatus() { return status; }
    public void setStatus(ArticleStatus status) { this.status = status; }
    public Account getAuthor() { return author; }
    public void setAuthor(Account author) { this.author = author; }
    public Account getReviewer() { return reviewer; }
    public void setReviewer(Account reviewer) { this.reviewer = reviewer; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public LocalDateTime getScheduledPublishAt() { return scheduledPublishAt; }
    public void setScheduledPublishAt(LocalDateTime scheduledPublishAt) {
        this.scheduledPublishAt = scheduledPublishAt;
    }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public long getViewCount() { return viewCount; }
    public void setViewCount(long viewCount) { this.viewCount = viewCount; }
    public long getRatingCount() { return ratingCount; }
    public void setRatingCount(long ratingCount) { this.ratingCount = ratingCount; }
    public long getRatingSum() { return ratingSum; }
    public void setRatingSum(long ratingSum) { this.ratingSum = ratingSum; }
    public BigDecimal getRatingAvg() { return ratingAvg; }
    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
    public Account getDeletedBy() { return deletedBy; }
    public void setDeletedBy(Account deletedBy) { this.deletedBy = deletedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    /** Chi co getter: xem chu thich o cot version. */
    public long getVersion() { return version; }
    public Set<Tag> getTags() { return tags; }
    public void setTags(Set<Tag> tags) { this.tags = tags; }
}
