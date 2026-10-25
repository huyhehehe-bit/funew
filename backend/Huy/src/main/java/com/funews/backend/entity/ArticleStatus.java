package com.funews.backend.entity;

/**
 * May trang thai 6 buoc cua bai viet - FR-ED-001.
 *
 * Luu xuong cot articles.status dang chuoi, nen TEN HANG phai khop chinh xac
 * rang buoc ck_articles_status trong module1_schema.sql. Doi ten hang o day
 * la phai doi ca rang buoc trong database.
 */
public enum ArticleStatus {
    /** Nhap, chi tac gia thay. */
    DRAFT,
    /** Da nop, khoa sua, cho bien tap. */
    PENDING_REVIEW,
    /** Bien tap tra lai kem ly do. */
    CHANGES_REQUESTED,
    /** Da duyet, cho toi gio dang. */
    SCHEDULED,
    /** Dang cong khai. */
    PUBLISHED,
    /** Go khoi trang cong khai, van giu trong database. */
    ARCHIVED
}
