package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.ApiException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.payload.CategoryDTO;
import com.ecommerce.project.payload.CategoryResponse;
import com.ecommerce.project.repositories.CategoryRepository;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class CategoryServiceImpl implements CategoryService {
    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    @Qualifier("modelMapper")
    private ModelMapper modelMapper;

    @Override
    public CategoryResponse getAllCategories(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sortByAndOrder = Objects.equals(sortOrder, "asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Category> categoryPage = categoryRepository.findAll(pageable);
        List<Category> categories = categoryPage.getContent();
        if (categories.isEmpty()) {
            throw new ApiException("No categories found");
        }
        List<CategoryDTO> categoryDtos = categories.stream()
                .map(category -> modelMapper.map(category, CategoryDTO.class))
                .toList();
        CategoryResponse categoryResponse = new CategoryResponse();
        categoryResponse.setContent(categoryDtos);
        categoryResponse.setPageNumber(categoryPage.getNumber());
        categoryResponse.setPageSize(categoryPage.getSize());
        categoryResponse.setTotalPages(categoryPage.getTotalPages());
        categoryResponse.setTotalElements(categoryPage.getTotalElements());
        categoryResponse.setLastPage(categoryPage.isLast());
        return categoryResponse;
    }

    @Override
    public CategoryDTO createCategory(@Valid CategoryDTO categoryDto) {
        Category foundCat = categoryRepository.findByCategoryName(categoryDto.getCategoryName());
        if (foundCat != null) {
            throw new ApiException("Category with name '" + categoryDto.getCategoryName() + "' already exists");
        }
        categoryDto.setCategoryId(null);
        Category category = modelMapper.map(categoryDto, Category.class);
        Category savedCategory = categoryRepository.save(category);
        return modelMapper.map(savedCategory, CategoryDTO.class);
    }

    @Override
    public CategoryDTO deleteCategory(Long categoryId) {
        Optional<Category> foundCat = categoryRepository.findById(categoryId);
        Category deleteCat = foundCat
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category", "categoryId", categoryId)
                );

        categoryRepository.delete(deleteCat);
        return modelMapper.map(deleteCat, CategoryDTO.class);
    }

    @Override
    public CategoryDTO updateCategory(@Valid CategoryDTO category, Long categoryId) {
        Optional<Category> categoryFound = categoryRepository.findById(categoryId);

        return categoryFound
                .map(cat -> {
                    cat.setCategoryName(category.getCategoryName());
                    Category savedCategory = categoryRepository.save(cat);
                    return modelMapper.map(savedCategory, CategoryDTO.class);
                }).orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", categoryId));
    }
}
