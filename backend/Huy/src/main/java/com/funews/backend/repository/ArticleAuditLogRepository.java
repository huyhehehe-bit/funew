package com.funews.backend.repository;

import com.funews.backend.entity.ArticleAuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Chi ghi them va doc - FR-ED-004, NFR-ED-004.
 *
 * JpaRepository co san delete() va saveAll(); dung goi chung o day. Tang database
 * cung chan bang trigger trg_audit_logs_immutable, nen co goi cung chi nhan loi.
 */
public interface ArticleAuditLogRepository extends JpaRepository<ArticleAuditLog, Long> {
    List<ArticleAuditLog> findByArticleIdOrderByCreatedAtDescIdDesc(Long articleId);
}
