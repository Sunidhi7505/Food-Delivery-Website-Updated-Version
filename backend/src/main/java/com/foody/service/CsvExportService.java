package com.foody.service;

import com.foody.dto.AdminUserResponse;
import com.foody.model.CartItem;
import com.foody.model.Order;
import com.foody.model.Product;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/** Builds CSV text for admin exports. Cells are quoted and neutralised against spreadsheet formula injection. */
@Service
public class CsvExportService {

    private final OrderService orderService;
    private final ProductService productService;
    private final UserService userService;

    public CsvExportService(OrderService orderService, ProductService productService, UserService userService) {
        this.orderService = orderService;
        this.productService = productService;
        this.userService = userService;
    }

    public String orders() {
        StringBuilder csv = header("Order ID", "Date", "Customer", "Email", "Items", "Subtotal", "Discount",
                "GST", "Delivery fee", "Total", "Payment", "Payment status", "Status");
        for (Order o : orderService.getAllOrders()) {
            String items = o.getItems() == null ? "" : o.getItems().stream()
                    .map((CartItem i) -> i.getName() + " x " + i.getQuantity())
                    .collect(Collectors.joining("; "));
            row(csv, o.getId(), o.getOrderTime(), o.getCustomerName(), o.getUserEmail(), items,
                    o.getSubtotalAmount(), o.getDiscountAmount(), o.getTaxAmount(), o.getDeliveryFee(),
                    o.getTotalAmount(), o.getPaymentMode(), o.getPaymentStatus(), o.getStatus());
        }
        return csv.toString();
    }

    public String products() {
        StringBuilder csv = header("ID", "Name", "Category", "Price", "Stock", "Vegetarian", "Available");
        List<Product> products = productService.getAll();
        for (Product p : products) {
            row(csv, p.getId(), p.getName(), p.getCategory(), p.getPrice(), p.getStock(), p.isVegetarian(),
                    p.isAvailable());
        }
        return csv.toString();
    }

    public String users() {
        StringBuilder csv = header("Name", "Email", "Phone", "Role", "Orders", "Total spent", "Blocked");
        for (AdminUserResponse u : userService.getUsersForAdmin()) {
            row(csv, u.getName(), u.getEmail(), u.getPhone(), u.getRole(), u.getOrderCount(), u.getTotalSpent(),
                    u.isBlocked());
        }
        return csv.toString();
    }

    static String cell(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        if (!text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0 && !isNumber(text)) {
            text = "'" + text;
        }
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private static boolean isNumber(String text) {
        try {
            Double.parseDouble(text);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private StringBuilder header(String... columns) {
        StringBuilder csv = new StringBuilder();
        row(csv, (Object[]) columns);
        return csv;
    }

    private void row(StringBuilder csv, Object... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(cell(values[i]));
        }
        csv.append("\r\n");
    }
}
