package com.funews.backend.entity;

/**
 * Loai thong bao gui cho nguoi dung.
 *
 * Luu xuong cot notifications.type dang chuoi, nen TEN HANG phai khop chinh xac
 * rang buoc ck_notifications_type trong module1_schema.sql. Them loai moi la
 * phai sua ca rang buoc ben database.
 */
public enum NotificationType {
    /** Gui cho bien tap: co bai moi nop, dang cho duyet. */
    SUBMITTED,
    /** Gui cho tac gia: bai bi tra lai kem ly do - AC-ED-002. */
    CHANGES_REQUESTED,
    /** Gui cho tac gia: bai da duoc duyet va hen gio dang. */
    SCHEDULED,
    /** Gui cho tac gia: bai da len trang cong khai. */
    PUBLISHED,
    /** Gui cho tac gia: bai da bi go khoi trang cong khai. */
    ARCHIVED
}
