package com.funews.backend.controller;

import com.funews.backend.dto.CategoryRequest;
import com.funews.backend.dto.CategoryResponse;
import com.funews.backend.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "3. Categories", description = "Quan ly danh muc tin tuc. Phu trach: Long")
@RestController
@RequestMapping("/api")
public class CategoryController {
    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @Operation(summary = "Danh sach danh muc - Admin va Staff deu doc duoc")
    @GetMapping("/categories")
    public List<CategoryResponse> categories() {
        return service.listCategories();
    }

    @Operation(summary = "Xem mot danh muc")
    @GetMapping("/categories/{id}")
    public CategoryResponse category(@PathVariable Long id) {
        return service.getCategory(id);
    }

    @Operation(summary = "Tao danh muc - chi Admin")
    @PostMapping("/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse created = service.createCategory(request);
        return ResponseEntity.created(URI.create("/api/categories/" + created.id())).body(created);
    }

    @Operation(summary = "Sua danh muc - khong cho tao vong lap cha con")
    @PutMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryResponse updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return service.updateCategory(id, request);
    }

    @Operation(summary = "Xoa danh muc - chan neu con danh muc con hoac bai viet")
    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        service.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
