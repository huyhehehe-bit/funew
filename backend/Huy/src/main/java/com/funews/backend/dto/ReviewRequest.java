package com.funews.backend.dto;

import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Thao tac cua bien tap vien tren mot bai dang cho duyet.
 *
 * note: ly do, BAT BUOC khi tra bai lai - FR-ED-002. Khi duyet bai thi khong can.
 * scheduledPublishAt: gio hen dang - FR-ED-003. De trong nghia la dang ngay.
 *
 * Khong co truong "action": moi hanh dong la mot endpoint rieng, nen khong the
 * goi sai hanh dong vi dien sai mot chu.
 */
public record ReviewRequest(@Size(max = 2000) String note, LocalDateTime scheduledPublishAt) {
}
