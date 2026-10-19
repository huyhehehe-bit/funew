package com.funews.backend.service;

import com.funews.backend.dto.ApiDtos.CategoryRequest;
import com.funews.backend.dto.ApiDtos.CategoryResponse;
import com.funews.backend.entity.Category;
import com.funews.backend.exception.ApiException;
import com.funews.backend.repository.CategoryRepository;
import com.funews.backend.repository.NewsArticleRepository;
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

    public List<CategoryResponse> listCategories() {
        return categories.findAllByOrderByIdAsc().stream().map(this::categoryResponse).toList();
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
        if (news.existsByCategoryId(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "This category contains news articles and cannot be deleted.");
        }
        categories.delete(category);
    }

    private void applyCategory(Category category, CategoryRequest request) {
        Category parent = request.parentId() == null || request.parentId() == 0
                ? null : requireCategory(request.parentId());
        Set<Long> visited = new HashSet<>();
        for (Category ancestor = parent; ancestor != null; ancestor = ancestor.getParent()) {
            if (ancestor.getId().equals(category.getId()) || !visited.add(ancestor.getId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Category hierarchy cannot contain a cycle.");
            }
        }
        category.setName(request.name().trim());
        category.setDescription(textOrEmpty(request.description()));
        category.setParent(parent);
        category.setIsActive(request.isActive());
    }

    private Category requireCategory(Long id) {
        return categories.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found."));
    }

    private CategoryResponse categoryResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getDescription(),
                category.getParent() == null ? 0L : category.getParent().getId(), category.getIsActive());
    }

    private String textOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
