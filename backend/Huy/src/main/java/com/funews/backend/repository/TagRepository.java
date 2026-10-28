package com.funews.backend.repository;

import com.funews.backend.entity.Tag;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findBySlugAndDeletedFalse(String slug);

    List<Tag> findByDeletedFalseOrderByNameAsc();
}
