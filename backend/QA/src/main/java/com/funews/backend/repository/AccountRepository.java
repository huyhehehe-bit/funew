package com.funews.backend.repository;

import com.funews.backend.entity.Account;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<Account> findAllByOrderByIdAsc();
    long countByRole(int role);
}
