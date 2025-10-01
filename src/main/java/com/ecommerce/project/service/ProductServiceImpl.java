package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.ApiException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Cart;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.payload.CartDto;
import com.ecommerce.project.payload.ProductDto;
import com.ecommerce.project.payload.ProductResponse;
import com.ecommerce.project.repositories.CartRepository;
import com.ecommerce.project.repositories.CategoryRepository;
import com.ecommerce.project.repositories.ProductRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {
    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ModelMapper modelMapper;


    @Autowired
    private FileService fileService;

    @Value("${project.image}")
    private String imageRootPath;

    @Value("${image.base.url}")
    private String imageBaseUrl;

    @Override
    public ProductDto addProduct(Long categoryId, ProductDto productDto) {

        Category category = categoryRepository.findById(categoryId).orElseThrow(
                () -> new ResourceNotFoundException("Category", "categoryid", categoryId)
        );
        boolean productIsNotPresent;
        List<Product> products = category.getProducts();
        productIsNotPresent = products.stream()
                .noneMatch(product -> product.getProductName().equals(productDto.getProductName()));
        if (productIsNotPresent) {

            Product newProduct = modelMapper.map(productDto, Product.class);
            newProduct.setCategory(category);
            newProduct.setSpecialPrice(productDto.getPrice() - productDto.getDiscount() * 0.01d * productDto.getPrice());
            Product savedProduct = productRepository.saveAndFlush(newProduct);

            return modelMapper.map(savedProduct, ProductDto.class);
        } else {
            throw new ApiException("Product already exists");
        }
    }

    @Override
    public ProductResponse getAllProducts(String keyword, String category, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sortByAndOrder = getSortByAndOrder(sortBy, sortOrder);

        Pageable pageable = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Specification<Product> spec = Specification.where(null);
        if (keyword != null && !keyword.isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("productName")), "%" + keyword.toLowerCase() + "%")
            );
        }
        if (category != null && !category.isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(root.get("category").get("categoryName"), category)
            );
        }
        Page<Product> productPage = productRepository.findAll(spec, pageable);
        return getProductResponse(productPage);
    }

    private String constructImageUrl(String imageName) {
        return imageBaseUrl.endsWith("/") ? imageBaseUrl + imageName : imageBaseUrl + "/" + imageName;
    }

    private static Sort getSortByAndOrder(String sortBy, String sortOrder) {
        Sort sortByAndOrder = Objects.equals(sortOrder, "asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return sortByAndOrder;
    }

    @Override
    public ProductResponse findProductsByCategory(Long categoryId, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Category category = categoryRepository.findById(categoryId).orElseThrow(
                () -> new ResourceNotFoundException("Category", "categoryid", categoryId)
        );
        Sort sortByAndOrder = getSortByAndOrder(sortBy, sortOrder);
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Product> productPage = productRepository.findByCategoryOrderByPriceAsc(category, pageable);
        return getProductResponse(productPage);
    }

    @Override
    public ProductResponse findProductsByKeyword(String keyword, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sortByAndOrder = getSortByAndOrder(sortBy, sortOrder);
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Product> productPage = productRepository.findProductsByProductNameContainingIgnoreCase(keyword, pageable);
        return getProductResponse(productPage);
    }

    private ProductResponse getProductResponse(Page<Product> productPage) {
        List<ProductDto> products = productPage
                .stream().map(product -> {
                    ProductDto prod = modelMapper.map(product, ProductDto.class);
                    prod.setImage(constructImageUrl(prod.getImage()));
                    return prod;
                })
                .toList();
        if (productPage.isEmpty()) {
            throw new ApiException("No productPage found");
        }
        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(products);
        productResponse.setPageNumber(productPage.getNumber());
        productResponse.setPageSize(productPage.getSize());
        productResponse.setTotalPages(productPage.getTotalPages());
        productResponse.setTotalElements(productPage.getTotalElements());
        productResponse.setLastPage(productPage.isLast());
        return productResponse;
    }

    @Override
    public ProductDto updateProduct(Long productId, ProductDto productDto) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productid", productId));
        product.setProductName(productDto.getProductName());
        product.setDescription(productDto.getDescription());
        product.setQuantity(productDto.getQuantity());
        product.setPrice(productDto.getPrice());
        product.setDiscount(productDto.getDiscount());
        product.setSpecialPrice(productDto.getPrice() - productDto.getDiscount() * 0.01d * productDto.getPrice());

        Product updatedProduct = productRepository.save(product);

        List<Cart> carts = cartRepository.findCartsByProductId(productId);

        List<CartDto> cartDTOs = carts.stream().map(cart -> {
            CartDto cartDTO = modelMapper.map(cart, CartDto.class);

            List<ProductDto> products = cart.getCartItems().stream()
                    .map(p -> modelMapper.map(p.getProduct(), ProductDto.class)).collect(Collectors.toList());

            cartDTO.setProducts(products);

            return cartDTO;

        }).collect(Collectors.toList());

        cartDTOs.forEach(cart -> cartService.updateProductInCarts(cart.getCartId(), productId));

        return modelMapper.map(updatedProduct, ProductDto.class);
    }

    @Override
    public ProductDto deleteProduct(Long productId) {
        ProductDto productDto = productRepository.findById(productId)
                .map(p -> modelMapper.map(p, ProductDto.class))
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productid", productId));

        // DELETE
        List<Cart> carts = cartRepository.findCartsByProductId(productId);
        carts.forEach(cart -> cartService.deleteProductFromCart(cart.getCartId(), productId));

        productRepository.deleteById(productId);
        return productDto;
    }

    @Override
    public ProductDto updateProductImage(Long productId, MultipartFile image) throws IOException, URISyntaxException {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productid", productId));

        String filename = fileService.uploadImage(imageRootPath, image);

        product.setImage(filename);
        productRepository.save(product);

        return modelMapper.map(product, ProductDto.class);
    }


}
