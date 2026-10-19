package com.funews.backend.repository;

import com.funews.backend.entity.Category;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByOrderByIdAsc();
    boolean existsByParentId(Long parentId);
}
