package com.funews.backend.repository;

import com.funews.backend.entity.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByOrderByIdAsc();
    List<Category> findByDeletedFalseOrderByIdAsc();
    long countByDeletedFalse();
    List<Category> findByActiveAndDeletedFalseOrderByIdAsc(boolean active);
    Optional<Category> findBySlugAndDeletedFalse(String slug);
    boolean existsByParentId(Long parentId);
    boolean existsByParentIdAndDeletedFalse(Long parentId);
    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);
    boolean existsByNameIgnoreCaseAndIdNotAndDeletedFalse(String name, Long id);
}
