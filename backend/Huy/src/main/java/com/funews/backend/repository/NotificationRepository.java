package com.funews.backend.repository;

import com.funews.backend.entity.Notification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByAccountIdAndDeletedFalseOrderByCreatedAtDescIdDesc(Long accountId);

    Optional<Notification> findByIdAndAccountIdAndDeletedFalse(Long id, Long accountId);

    long countByAccountIdAndReadFalseAndDeletedFalse(Long accountId);
}
