package com.foody.controller;

import com.foody.dto.AdminUserResponse;
import com.foody.model.Product;
import com.foody.service.CsvExportService;
import com.foody.service.OrderService;
import com.foody.service.ProductService;
import com.foody.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final OrderService orderService;
    private final ProductService productService;
    private final UserService userService;
    private final CsvExportService csvExportService;

    public AdminController(OrderService orderService,
                           ProductService productService,
                           UserService userService,
                           CsvExportService csvExportService) {
        this.orderService = orderService;
        this.productService = productService;
        this.userService = userService;
        this.csvExportService = csvExportService;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboardStats() {

        Map<String, Object> analytics = orderService.getAnalytics();
        return Map.of(
                "totalRevenue", orderService.getTotalRevenue(),
                "totalOrders", orderService.getAllOrders().size(),
                "totalProducts", productService.getAll().size(),
                "analytics", analytics
        );
    }

    @GetMapping("/export/{type}.csv")
    public ResponseEntity<byte[]> exportCsv(@PathVariable String type) {
        String csv = switch (type) {
            case "orders" -> csvExportService.orders();
            case "products" -> csvExportService.products();
            case "users" -> csvExportService.users();
            default -> throw new com.foody.exception.ResourceNotFoundException("Unknown export: " + type);
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"foody-" + type + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @PostMapping("/seed-products")
    public List<Product> seedProducts() {
        return productService.seedProducts();
    }

    @GetMapping("/users")
    public List<AdminUserResponse> users() {
        return userService.getUsersForAdmin();
    }

    @PutMapping("/users/{userId}/block")
    public AdminUserResponse blockUser(@PathVariable String userId,
                                       @RequestParam boolean blocked) {
        return userService.setBlocked(userId, blocked);
    }
}
