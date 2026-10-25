package com.foody.controller;

import com.foody.dto.PageResponse;
import com.foody.dto.ProductRequest;
import com.foody.model.Product;
import jakarta.validation.Valid;
import com.foody.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public Product create(@Valid @RequestBody ProductRequest request) {
        return productService.create(request.toProduct());
    }

    @PutMapping("/{id}")
    public Product update(@PathVariable String id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request.toProduct());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        productService.delete(id);
    }

    @GetMapping
    public List<Product> getAll() {
        return productService.getAll();
    }

    @GetMapping("/featured")
    public List<Product> featured() {
        return productService.getFeatured();
    }

    @GetMapping("/{id}")
    public Product getById(@PathVariable String id) {
        return productService.getById(id);
    }

    @GetMapping("/search")
    public List<Product> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean vegetarian
    ) {
        return productService.searchFilterSort(
                keyword,
                available,
                minPrice,
                maxPrice,
                sortBy,
                direction,
                category,
                vegetarian
        );
    }

    @GetMapping("/page")
    public PageResponse<Product> searchPage(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean vegetarian,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return PageResponse.of(productService.searchFilterSort(
                keyword, available, minPrice, maxPrice, sortBy, direction, category, vegetarian), page, size);
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return productService.getCategories();
    }
}
