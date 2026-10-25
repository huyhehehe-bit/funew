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
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Tai khoan - bang accounts.
 *
 * XOA MEM: khong bao gio DELETE, chi dat deleted = true kem deletedAt va deletedBy.
 * Vi vay email KHONG dat unique = true o day: rang buoc that la filtered index
 * ux_accounts_email (WHERE is_deleted = 0) trong module1_schema.sql, nho the xoa mem
 * roi tao lai cung email van duoc.
 *
 * deletedBy tro ve chinh bang accounts: luu ai da xoa tai khoan nay.
 *
 * role khong con la so nguyen 1/2 nhu truoc, ma la khoa ngoai tro ve bang roles.
 */
@Entity
@Table(name = "accounts")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 254)
    private String username;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /** status: 0 = inactive, 1 = active. */
    @Column(name = "status", nullable = false)
    private boolean active = true;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by_id")
    private Account deletedBy;

    /* Hai cot thoi gian do Hibernate tu dien, khong phai viec cua service.
       Database co DEFAULT SYSDATETIME() nhung Hibernate luon gui gia tri tuong minh
       trong cau INSERT nen DEFAULT do khong bao gio chay. */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Account() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getStatus() { return active ? 1 : 0; }
    public void setStatus(int status) {
        if (status != 0 && status != 1) {
            throw new IllegalArgumentException("Status must be 0 or 1.");
        }
        this.active = status == 1;
    }
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
}
