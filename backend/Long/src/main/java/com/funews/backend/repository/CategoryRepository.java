package com.funews.backend.repository;

import com.funews.backend.entity.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByOrderByIdAsc();
    List<Category> findByDeletedFalseOrderByIdAsc();
    /* Loc theo trang thai. Cot trong database la is_active nhung thuoc tinh ben
       entity ten la active va kieu boolean, nen ten phuong thuc phai la
       findByActive... - de findByIsActive... la Spring Data khong tim thay
       thuoc tinh va ung dung chet ngay luc khoi dong. */
    List<Category> findByActiveAndDeletedFalseOrderByIdAsc(boolean active);
    Optional<Category> findBySlugAndDeletedFalse(String slug);
    boolean existsByParentId(Long parentId);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
