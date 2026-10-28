package com.funews.backend.repository;

import com.funews.backend.entity.Account;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, Long> {
    @EntityGraph(attributePaths = "role")
    @Query("select a from Account a where lower(a.email) = lower(:email) and a.deleted = false")
    Optional<Account> findByEmailIgnoreCase(@Param("email") String email);

    @EntityGraph(attributePaths = "role")
    Optional<Account> findByUsernameIgnoreCaseAndDeletedFalse(String username);

    @Override
    @EntityGraph(attributePaths = "role")
    Optional<Account> findById(Long id);

    @EntityGraph(attributePaths = "role")
    List<Account> findByDeletedFalseOrderByIdAsc();

    boolean existsByEmailIgnoreCaseAndDeletedFalse(String email);

    long countByRole_CodeAndActiveTrueAndDeletedFalse(String roleCode);

    @Query("select count(article) from NewsArticle article where article.author.id = :accountId")
    long countArticlesByAuthorId(@Param("accountId") Long accountId);
}
