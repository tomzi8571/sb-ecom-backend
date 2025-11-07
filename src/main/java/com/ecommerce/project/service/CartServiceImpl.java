package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.ApiException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Cart;
import com.ecommerce.project.model.CartItem;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.payload.CartDto;
import com.ecommerce.project.payload.CartItemDto;
import com.ecommerce.project.payload.ProductDto;
import com.ecommerce.project.repositories.CartItemRepository;
import com.ecommerce.project.repositories.CartRepository;
import com.ecommerce.project.repositories.ProductRepository;
import com.ecommerce.project.util.AuthUtil;
import io.jsonwebtoken.lang.Objects;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private AuthUtil authUtil;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private EntityManager entityManager;

    @Override
    public CartDto addProductToCart(Long productId, Integer quantity) {
        Cart cart = fetchOrCreateCart();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));


        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cart.getCartId(), productId);
        if (cartItem != null) {
            throw new ApiException("Product " + product.getProductName() + " already exists in cart");
        }

        if (product.getQuantity() == 0) {
            throw new ApiException("Product " + product.getProductName() + " ist not avaiable");
        }

        if (product.getQuantity() < quantity) {
            throw new ApiException("Product " + product.getProductName() + " must be less than " + product.getQuantity());
        }

        CartItem newCartItem = new CartItem();
        newCartItem.setProduct(product);
        newCartItem.setCart(cart);
        newCartItem.setQuantity(quantity);
        newCartItem.setDiscount(product.getDiscount());
        newCartItem.setProductPrice(product.getPrice());
        product.setQuantity(product.getQuantity());

        cartItemRepository.save(newCartItem);
        cart.setTotalPrice(cart.getTotalPrice() + product.getSpecialPrice() * quantity);
        cart.getCartItems().add(newCartItem);
        cartRepository.save(cart);

        return convertToCartDto(cart);
    }

    private CartDto convertToCartDto(Cart cart) {
        CartDto cartDto = modelMapper.map(cart, CartDto.class);
        List<ProductDto> productDtos = cart.getCartItems()
                .stream().map((item) -> {
                    ProductDto prod = modelMapper.map(item.getProduct(), ProductDto.class);
                    prod.setQuantity(item.getQuantity());
                    return prod;
                }).toList();
        cartDto.setProducts(productDtos);
        return cartDto;
    }

    @Override
    public List<CartDto> getAllCarts() {
        List<Cart> allCarts = cartRepository.findAll();
        if (allCarts.isEmpty()) {
            throw new ApiException("No cart exists");
        }
        List<CartDto> allCartDtos = allCarts.stream()
                .map(this::convertToCartDto)
                .toList();
        System.out.println("allCartDtos: " + allCartDtos);
        return allCartDtos;
    }

    @Override
    public CartDto getCart(String userEmail, Long cartId) {
        Cart cart = cartRepository.findCartByEmailAndCartId(userEmail, cartId);
        if (cart == null) {
            throw new ResourceNotFoundException("Cart", "cartId", cartId);
        }

        CartDto cartDto = convertToCartDto(cart);
        cart.getCartItems().forEach(c ->
                c.getProduct().setQuantity(c.getQuantity()));
        return cartDto;
    }

    @Transactional
    @Override
    public CartDto updateProductQuantityInCart(Long productId, Integer quantity) {

        String emailId = authUtil.loggedInEmail();
        Cart userCart = cartRepository.findCartByEmail(emailId);
        Long cartId = userCart.getCartId();

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        if (product.getQuantity() == 0) {
            throw new ApiException(product.getProductName() + " is not available");
        }

        if (product.getQuantity() < quantity) {
            throw new ApiException("Please, make an order of the " + product.getProductName()
                    + " less than or equal to the quantity " + product.getQuantity() + ".");
        }

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId, productId);

        if (cartItem == null) {
            throw new ApiException("Product " + product.getProductName() + " not available in the cart!!!");
        }

        // Calculate new quantity
        int newQuantity = cartItem.getQuantity() + quantity;

        // Validation to prevent negative quantities
        if (newQuantity < 0) {
            throw new ApiException("The resulting quantity cannot be negative.");
        }

        if (newQuantity == 0) {
            cart.setTotalPrice(cart.getTotalPrice() -
                    (cartItem.getProductPrice() * cartItem.getQuantity()));
            deleteProductFromCart(cartItem);
        } else {
            cartItem.setProductPrice(product.getSpecialPrice());
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
            cartItem.setDiscount(product.getDiscount());
            cart.setTotalPrice(cart.getTotalPrice() + (cartItem.getProductPrice() * quantity));
            cartItemRepository.save(cartItem);
        }

        cartRepository.save(cart);
        CartDto cartDTO = modelMapper.map(cart, CartDto.class);

        List<CartItem> cartItems = cart.getCartItems();

        Stream<ProductDto> productStream = cartItems.stream().map(item -> {
            ProductDto prd = modelMapper.map(item.getProduct(), ProductDto.class);
            prd.setQuantity(item.getQuantity());
            return prd;
        });


        cartDTO.setProducts(productStream.toList());

        return cartDTO;
    }

    @Override
    public String deleteProductFromCart(Long cartId, Long productId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId, productId);

        if (cartItem == null) {
            throw new ResourceNotFoundException("Product", "productId", productId);
        }

        cart.setTotalPrice(cart.getTotalPrice() -
                (cartItem.getProductPrice() * cartItem.getQuantity()));

        cartItemRepository.deleteByIdCustom(cartItem.getCartItemId());

        return "Product " + cartItem.getProduct().getProductName() + " removed from the cart !!!";

    }

    @Override
    public void updateProductInCarts(Long cartId, Long productId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId, productId);

        if (cartItem == null) {
            throw new ApiException("Product " + product.getProductName() + " not available in the cart!!!");
        }

        // 1000-100*2=800
        double cartPrice = cart.getTotalPrice()
                - (cartItem.getProductPrice() * cartItem.getQuantity());

        // 200
        cartItem.setProductPrice(product.getSpecialPrice());

        // 800 + 200*2 = 1200
        cart.setTotalPrice(cartPrice
                + (cartItem.getProductPrice() * cartItem.getQuantity()));

        cartItemRepository.save(cartItem);
    }

    @Override
    @Transactional
    public String createOrUpdateCartWithItems(List<CartItemDto> cartItems) {
        Cart cart = fetchOrCreateCart();

        // Clear all current items in the existing cart
        if (!Objects.isEmpty(cart.getCartItems())) {
            cartItemRepository.deleteAllByCartId(cart.getCartId());
            cart.getCartItems().clear();
            cartRepository.save(cart);
        }
        double totalPrice = 0.00;
        // Process each item in the request to add the cart
        for (CartItemDto cartItem : cartItems) {
            Long productId = cartItem.getProductId();
            Integer quantity = cartItem.getQuantity();
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

            // Lets update this after we checked it after it was ordered and payed.
            // product.setQuantity(product.getQuantity() - quantity);
            totalPrice += product.getSpecialPrice() * quantity;

            CartItem newCartItem = new CartItem();
            newCartItem.setProduct(product);
            newCartItem.setCart(cart);
            newCartItem.setQuantity(quantity);
            newCartItem.setDiscount(product.getDiscount());
            newCartItem.setProductPrice(product.getSpecialPrice());
            cartItemRepository.save(newCartItem);
            cart.getCartItems().add(newCartItem);
        }

        cart.setTotalPrice(totalPrice);
        cartRepository.save(cart);

        return "Cart created or updated successfully";
    }

    String deleteProductFromCart(CartItem cartItem) {
        cartItemRepository.deleteByIdCustom(cartItem.getCartItemId());
        return "Product " + cartItem.getProduct().getProductName() + " removed from the cart !!!";
    }

    private Cart fetchOrCreateCart() {
        Cart cart = cartRepository.findCartByEmail(authUtil.loggedInEmail());
        if (cart != null) {
            return cart;
        }

        cart = new Cart();
        cart.setTotalPrice(0.00);
        cart.setUser(authUtil.loggedInUser());
        return cartRepository.save(cart);
    }
}
