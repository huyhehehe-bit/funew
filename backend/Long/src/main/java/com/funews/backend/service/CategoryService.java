package com.funews.backend.service;

import com.funews.backend.dto.CategoryRequest;
import com.funews.backend.dto.CategoryResponse;
import com.funews.backend.entity.Category;
import com.funews.backend.exception.ApiException;
import com.funews.backend.repository.CategoryRepository;
import com.funews.backend.repository.NewsArticleRepository;
import com.funews.backend.util.Slugs;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository categories;
    private final NewsArticleRepository news;

    public CategoryService(CategoryRepository categories, NewsArticleRepository news) {
        this.categories = categories;
        this.news = news;
    }

    /**
     * status = null: lay tat ca. true: chi danh muc dang bat. false: chi danh muc da tat.
     *
     * Cot is_active da doi tu INT 0/1 sang BIT nen o day la boolean, khong con
     * quy doi ra 1/0 nua. Ca hai nhanh deu bo qua danh muc da xoa mem.
     */
    public List<CategoryResponse> listCategories(Boolean status) {
        List<Category> categoryList = status == null
                ? categories.findByDeletedFalseOrderByIdAsc()
                : categories.findByActiveAndDeletedFalseOrderByIdAsc(status);

        return categoryList.stream()
                .map(this::categoryResponse)
                .toList();
    }


    public CategoryResponse getCategory(Long id) {
        return categoryResponse(requireCategory(id));
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        Category category = new Category();
        applyCategory(category, request);
        return categoryResponse(categories.save(category));
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = requireCategory(id);
        applyCategory(category, request);
        return categoryResponse(categories.save(category));
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = requireCategory(id);
        if (categories.existsByParentId(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "This category has child categories and cannot be deleted.");
        }
        if (news.existsByCategoryIdAndDeletedFalse(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "This category contains news articles and cannot be deleted.");
        }
        categories.delete(category);
    }

    private void applyCategory(Category category, CategoryRequest request) {
        validateDuplicateName(
                request.name(),
                category.getId()
        );
        Category parent = request.parentId() == null || request.parentId() == 0
                ? null : requireCategory(request.parentId());
        Set<Long> visited = new HashSet<>();
        for (Category ancestor = parent;
             ancestor != null;
             ancestor = ancestor.getParent()) {
            if (ancestor.getId().equals(category.getId())
                    || !visited.add(ancestor.getId())) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Category hierarchy cannot contain a cycle."
                );
            }
        }
        category.setName(request.name().trim());
        category.setSlug(uniqueSlug(request.slug(), request.name(), category.getId()));
        category.setDescription(textOrEmpty(request.description()));
        category.setParent(parent);
        category.setDisplayOrder(request.displayOrder() == null ? 0 : request.displayOrder());
        category.setActive(Boolean.TRUE.equals(request.isActive()));
    }

    /* Cot slug la NOT NULL va co filtered unique index, nen phai tu sinh khi client
       de trong, va tu them -2, -3 khi trung ten voi danh muc khac. */
    private String uniqueSlug(String requested, String name, Long currentId) {
        String base = Slugs.of(requested == null || requested.isBlank() ? name : requested, "danh-muc");
        String candidate = base;
        for (int suffix = 2; isSlugTaken(candidate, currentId); suffix++) {
            candidate = base + "-" + suffix;
        }
        return candidate;
    }

    private boolean isSlugTaken(String slug, Long currentId) {
        return categories.findBySlugAndDeletedFalse(slug)
                .filter(other -> !other.getId().equals(currentId)).isPresent();
    }

    private Category requireCategory(Long id) {
        return categories.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found."));
    }

    private CategoryResponse categoryResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getSlug(),
                category.getDescription(), category.getParent() == null ? 0L : category.getParent().getId(),
                category.getDisplayOrder(), category.isActive());
    }

    private String textOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private void validateDuplicateName(String name, Long id) {
        if (id == null) {
            if (categories.existsByNameIgnoreCase(name.trim())) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "Category name already exists.");
            }
        } else {
            if (categories.existsByNameIgnoreCaseAndIdNot(name.trim(), id)) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "Category name already exists.");
            }
        }
    }
}
