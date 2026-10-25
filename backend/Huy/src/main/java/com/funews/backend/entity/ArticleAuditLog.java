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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;

/**
 * Nhat ky kiem toan - bang article_audit_logs. FR-ED-004 va NFR-ED-004.
 *
 * Ghi lai moi lan doi trang thai bai viet. Log phai BAT BIEN: bang nay co tinh
 * KHONG co cot xoa mem va khong co updatedAt - khong ai duoc sua hay xoa, ke ca
 * Administrator. @Immutable bat Hibernate bo qua moi lenh UPDATE len entity nay,
 * nen chi co duong ghi them.
 *
 * Khi toStatus = CHANGES_REQUESTED thi note BAT BUOC co - rang buoc
 * ck_audit_note_required o tang database, khong phu thuoc tang ung dung.
 */
@Entity
@Immutable
@Table(name = "article_audit_logs")
public class ArticleAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    private NewsArticle article;

    /** Null o dong dau tien cua mot bai viet. */
    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private ArticleStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private ArticleStatus toStatus;

    /* Database khong rang buoc tap gia tri cua cot nay nen de String, khong de enum:
       them mot hanh dong moi se khong lam cu INSERT nao do vo. Cac gia tri dang dung:
       SUBMIT, REQUEST_CHANGES, APPROVE, SCHEDULE, PUBLISH, ARCHIVE. */
    @Column(nullable = false, length = 30)
    private String action;

    /** Ly do tra bai, hoac ghi chu cua bien tap. */
    @Column(length = 2000)
    private String note;

    /** Nguoi thuc hien hanh dong. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    private Account actor;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ArticleAuditLog() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public NewsArticle getArticle() { return article; }
    public void setArticle(NewsArticle article) { this.article = article; }
    public ArticleStatus getFromStatus() { return fromStatus; }
    public void setFromStatus(ArticleStatus fromStatus) { this.fromStatus = fromStatus; }
    public ArticleStatus getToStatus() { return toStatus; }
    public void setToStatus(ArticleStatus toStatus) { this.toStatus = toStatus; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Account getActor() { return actor; }
    public void setActor(Account actor) { this.actor = actor; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
