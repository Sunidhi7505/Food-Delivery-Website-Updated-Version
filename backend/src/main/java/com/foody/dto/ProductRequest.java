package com.foody.dto;

import com.foody.model.Product;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Validated payload for creating or updating a product (admin). */
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 80, message = "Name must be 2 to 80 characters")
    private String name;

    @Size(max = 500, message = "Description can be at most 500 characters")
    private String description;

    @Size(max = 40, message = "Category can be at most 40 characters")
    private String category;

    @DecimalMin(value = "0.0", message = "Price cannot be negative")
    @DecimalMax(value = "100000.0", message = "Price is too high")
    private double price;

    @Size(max = 500, message = "Image URL is too long")
    @Pattern(regexp = "^$|^https?://.+", message = "Image URL must start with http:// or https://")
    private String imageUrl;

    @Min(value = 0, message = "Stock cannot be negative")
    @Max(value = 100000, message = "Stock is too high")
    private int stock;

    private boolean vegetarian;

    @Min(value = 5, message = "Preparation time must be at least 5 minutes")
    @Max(value = 240, message = "Preparation time must be at most 240 minutes")
    private int preparationTimeMinutes = 25;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public boolean isVegetarian() {
        return vegetarian;
    }

    public void setVegetarian(boolean vegetarian) {
        this.vegetarian = vegetarian;
    }

    public int getPreparationTimeMinutes() {
        return preparationTimeMinutes;
    }

    public void setPreparationTimeMinutes(int preparationTimeMinutes) {
        this.preparationTimeMinutes = preparationTimeMinutes;
    }

    public Product toProduct() {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setCategory(category);
        product.setPrice(price);
        product.setImageUrl(imageUrl);
        product.setStock(stock);
        product.setVegetarian(vegetarian);
        product.setPreparationTimeMinutes(preparationTimeMinutes);
        return product;
    }
}
