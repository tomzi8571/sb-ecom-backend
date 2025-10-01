package com.ecommerce.project.controller;

import com.ecommerce.project.payload.ProductDto;
import com.ecommerce.project.payload.ProductResponse;
import com.ecommerce.project.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URISyntaxException;

import static com.ecommerce.project.config.AppConstants.*;

@RestController
@RequestMapping("/api")
public class ProductController {

    @Autowired
    private ProductService productService;

    @PostMapping("/admin/categories/{categoryId}/product")
    public ResponseEntity<ProductDto> addProduct(@Valid @RequestBody ProductDto productDto, @PathVariable Long categoryId) {
        ProductDto newProductDto = productService.addProduct(categoryId, productDto);
        return new ResponseEntity<>(newProductDto, HttpStatus.CREATED);
    }

    @PutMapping("/admin/products/{productId}")
    public ResponseEntity<ProductDto> updateProduct(@Valid @RequestBody ProductDto productDto, @PathVariable Long productId) {
        ProductDto updatedProductDto = productService.updateProduct(productId, productDto);
        return new ResponseEntity<>(updatedProductDto, HttpStatus.CREATED);
    }

    @GetMapping("/public/products")
    public ResponseEntity<ProductResponse> getAllProducts(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "pageNumber", defaultValue = PAGE_NUMBER) Integer pageNumber,
            @RequestParam(value = "pageSize", defaultValue = PAGE_SIZE) Integer pageSize,
            @RequestParam(value = "sortBy", defaultValue = SORT_PRODUCTS_BY) String sortBy,
            @RequestParam(value = "sortOrder", defaultValue = SORT_DIR) String sortOrder
    ) {
        ProductResponse products = productService.getAllProducts(keyword, category, pageNumber, pageSize, sortBy, sortOrder);
        return ResponseEntity.ok(products);
    }

    @DeleteMapping("/admin/products/{productId}")
    public ResponseEntity<ProductDto> deleteProduct(@PathVariable Long productId) {
        ProductDto deletedProduct = productService.deleteProduct(productId);
        return ResponseEntity.ok(deletedProduct);
    }

    @GetMapping("/public/categories/{categoryId}/products")
    public ResponseEntity<ProductResponse> getProductsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(value = "pageNumber", defaultValue = PAGE_NUMBER) Integer pageNumber,
            @RequestParam(value = "pageSize", defaultValue = PAGE_SIZE) Integer pageSize,
            @RequestParam(value = "sortBy", defaultValue = SORT_PRODUCTS_BY) String sortBy,
            @RequestParam(value = "sortOrder", defaultValue = SORT_DIR) String sortOrder
    ) {
        ProductResponse productResponse = productService.findProductsByCategory(categoryId, pageNumber, pageSize, sortBy, sortOrder);


        return ResponseEntity.ok(productResponse);
    }

    @GetMapping("/public/products/keyword/{keyword}")
    public ResponseEntity<ProductResponse> getProductsByKeyword(
            @PathVariable String keyword,
            @RequestParam(value = "pageNumber", defaultValue = PAGE_NUMBER) Integer pageNumber,
            @RequestParam(value = "pageSize", defaultValue = PAGE_SIZE) Integer pageSize,
            @RequestParam(value = "sortBy", defaultValue = SORT_PRODUCTS_BY) String sortBy,
            @RequestParam(value = "sortOrder", defaultValue = SORT_DIR) String sortOrder
    ) {
        ProductResponse products = productService.findProductsByKeyword(keyword, pageNumber, pageSize, sortBy, sortOrder);
        return ResponseEntity.ok(products);
    }

    @PutMapping("/products/{productId}/image")
    public ResponseEntity<ProductDto> updateProductImage(@PathVariable Long productId
            , @RequestParam("image") MultipartFile image) throws IOException, URISyntaxException {
        ProductDto updatedProduct = productService.updateProductImage(productId, image);
        return ResponseEntity.ok(updatedProduct);
    }
}
