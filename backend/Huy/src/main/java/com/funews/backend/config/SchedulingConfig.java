package com.funews.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Bat tac vu dinh ky cho ScheduledPublisher - AC-ED-003 doi bai tu len trang khi
 * den gio hen, khong ai bam.
 *
 * De o folder Huy chu khong gan vao BackendApplication cua shared, de khong phai
 * sua file dung chung.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
