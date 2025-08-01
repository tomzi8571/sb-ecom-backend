package com.ecommerce.project.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    private Long productId;

    @NotBlank
    @Size(min = 3, message = "Product must have at least 3 charecters")
    private String productName;
    private String image;
    @Size(min=10, message = "Product must have at least 10 charecters")
    private String description;
    private Integer quantity;
    private double price;
    private double discount;
    private double specialPrice;

}
