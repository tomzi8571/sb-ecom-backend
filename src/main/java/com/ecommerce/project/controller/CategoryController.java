package com.ecommerce.project.controller;

import com.ecommerce.project.payload.CategoryDTO;
import com.ecommerce.project.payload.CategoryResponse;
import com.ecommerce.project.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.ecommerce.project.config.AppConstants.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Category APIs", description = "API's for managing Categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @Operation(summary = "Get all Categories", description = "API to get all categories")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Categories correctly retrieved")
    })
    @GetMapping("public/categories")
    public ResponseEntity<CategoryResponse> getAllCategories(
            @RequestParam(value = "pageNumber", defaultValue = PAGE_NUMBER) Integer pageNumber,
            @RequestParam(value = "pageSize", defaultValue = PAGE_SIZE) Integer pageSize,
            @RequestParam(value = "sortBy", defaultValue = SORT_CATEGORIES_BY) String sortBy,
            @RequestParam(value = "sortOrder", defaultValue = SORT_DIR) String sortOrder
    ) {
        return ResponseEntity.ok(categoryService.getAllCategories(pageNumber, pageSize, sortBy, sortOrder));
    }

    @Operation(summary = "Create Categories", description = "API to get create categories")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Categories correctly created"),
            @ApiResponse(responseCode = "400", description = "Invalid Input", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @PostMapping("public/categories")
    ResponseEntity<CategoryDTO> createCategory(
            @Valid @RequestBody CategoryDTO category) {
        CategoryDTO createdCategory = categoryService.createCategory(category);
        return ResponseEntity.ok(createdCategory);
    }

    @DeleteMapping(value = "admin/categories/{categoryId}")
    public ResponseEntity<CategoryDTO> deleteCategory(
            @Parameter(description = "Category we wish to delete")
            @PathVariable Long categoryId) {
        return ResponseEntity.ok(categoryService.deleteCategory(categoryId));
    }

    @PutMapping("public/categories/{categoryId}")
    ResponseEntity<CategoryDTO> updateCategory(@Valid @RequestBody CategoryDTO category, @PathVariable Long categoryId) {
        CategoryDTO savedCategory = categoryService.updateCategory(category, categoryId);
        return ResponseEntity.ok(savedCategory);
    }
}
