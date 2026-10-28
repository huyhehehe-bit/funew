package com.funews.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Quy tac bien tap - bang editorial_rules. SRS muc 3.2: Administrator
 * "configures editorial rules".
 *
 * Bang nay CO DUNG MOT DONG, id luon bang 1. Khong dung IDENTITY vi khong bao
 * gio them dong thu hai: cau hinh cua he thong thi chi co mot ban.
 *
 * Moi quy tac o day deu duoc TANG SERVICE AP DUNG THAT, khong phai cau hinh de
 * trang tri - xem NewsService: minSummaryLength chan luc nop bai,
 * requireNoteOnApprove chan luc duyet, autoPublishScheduled tat tac vu hen gio.
 */
@Entity
@Table(name = "editorial_rules")
public class EditorialRules {
    public static final long SINGLE_ROW_ID = 1L;

    @Id
    private Long id = SINGLE_ROW_ID;

    /** Tat di thi bai hen gio nam yen cho bien tap bam Dang, khong tu len trang. */
    @Column(name = "auto_publish_scheduled", nullable = false)
    private boolean autoPublishScheduled = true;

    /** Bat len thi duyet bai cung phai ghi chu, khong chi tra bai moi phai. */
    @Column(name = "require_note_on_approve", nullable = false)
    private boolean requireNoteOnApprove = false;

    /** So ky tu tom tat toi thieu de duoc nop bai. 0 nghia la khong bat buoc. */
    @Column(name = "min_summary_length", nullable = false)
    private int minSummaryLength = 0;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_id")
    private Account updatedBy;

    public EditorialRules() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public boolean isAutoPublishScheduled() { return autoPublishScheduled; }
    public void setAutoPublishScheduled(boolean autoPublishScheduled) {
        this.autoPublishScheduled = autoPublishScheduled;
    }
    public boolean isRequireNoteOnApprove() { return requireNoteOnApprove; }
    public void setRequireNoteOnApprove(boolean requireNoteOnApprove) {
        this.requireNoteOnApprove = requireNoteOnApprove;
    }
    public int getMinSummaryLength() { return minSummaryLength; }
    public void setMinSummaryLength(int minSummaryLength) { this.minSummaryLength = minSummaryLength; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Account getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Account updatedBy) { this.updatedBy = updatedBy; }
}
