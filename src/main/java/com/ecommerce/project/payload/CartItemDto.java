package com.ecommerce.project.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDto {
    private Long cartItemId;
    private CartDto cart;
    private ProductDto product;
    private double discount;
    private Integer quantity;
    private double productPrice;
}
