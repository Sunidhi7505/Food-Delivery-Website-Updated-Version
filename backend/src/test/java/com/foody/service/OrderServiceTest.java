package com.foody.service;

import com.foody.dto.CouponValidationResponse;
import com.foody.dto.PlaceOrderRequest;
import com.foody.model.Address;
import com.foody.model.Cart;
import com.foody.model.CartItem;
import com.foody.model.Product;
import com.foody.model.User;
import com.foody.repository.CartRepository;
import com.foody.repository.OrderRepository;
import com.foody.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock CartRepository cartRepository;
    @Mock ProductRepository productRepository;
    @Mock UserService userService;
    @Mock RestaurantInfoService restaurantInfoService;
    @Mock NotificationService notificationService;
    @Mock PaymentService paymentService;
    @Mock CouponService couponService;
    @Mock PricingService pricingService;
    @InjectMocks OrderService orderService;

    @Test
    void placeOrderFailsForEmptyCart() {
        Cart cart = new Cart();
        cart.setItems(new ArrayList<>());
        when(cartRepository.findByUserEmail("a@b.com")).thenReturn(Optional.of(cart));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> orderService.placeOrder("a@b.com", null));

        assertEquals("Cart is empty", ex.getMessage());
    }

    /** Regression: an invalid coupon must not leave stock reduced. */
    @Test
    void invalidCouponDoesNotReduceStock() {
        CartItem item = new CartItem();
        item.setProductId("p1");
        item.setQuantity(2);
        Cart cart = new Cart();
        cart.setItems(new ArrayList<>(List.of(item)));

        Product product = new Product();
        ReflectionTestUtils.setField(product, "id", "p1");
        product.setName("Pizza");
        product.setPrice(100);
        product.setStock(5);
        product.setAvailable(true);

        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setCouponCode("BAD");
        Address address = new Address();
        address.setLine1("Street 1");
        request.setDeliveryAddress(address);

        when(cartRepository.findByUserEmail("a@b.com")).thenReturn(Optional.of(cart));
        when(restaurantInfoService.isOpenNow()).thenReturn(true);
        when(userService.getUserByEmail("a@b.com")).thenReturn(new User());
        when(productRepository.findById("p1")).thenReturn(Optional.of(product));
        when(couponService.validate("BAD", 200))
                .thenReturn(new CouponValidationResponse(false, "Coupon not found", "BAD", 200, 0));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> orderService.placeOrder("a@b.com", request));

        assertEquals("Coupon not found", ex.getMessage());
        assertEquals(5, product.getStock());
        verify(productRepository, never()).save(any(Product.class));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void placeOrderFailsWhenRestaurantClosed() {
        CartItem item = new CartItem();
        item.setProductId("p1");
        item.setQuantity(1);
        Cart cart = new Cart();
        cart.setItems(new ArrayList<>(List.of(item)));
        com.foody.model.RestaurantInfo info = new com.foody.model.RestaurantInfo();
        when(cartRepository.findByUserEmail("a@b.com")).thenReturn(Optional.of(cart));
        when(restaurantInfoService.isOpenNow()).thenReturn(false);
        when(restaurantInfoService.getRestaurant()).thenReturn(info);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> orderService.placeOrder("a@b.com", null));

        assertTrue(ex.getMessage().startsWith("Restaurant is closed"));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void reorderSkipsUnavailableDishesAndCapsToStock() {
        CartItem pizza = new CartItem();
        pizza.setProductId("p1");
        pizza.setName("Pizza");
        pizza.setQuantity(3);
        CartItem soup = new CartItem();
        soup.setProductId("p2");
        soup.setName("Soup");
        soup.setQuantity(1);
        com.foody.model.Order past = new com.foody.model.Order();
        past.setUserEmail("a@b.com");
        past.setItems(List.of(pizza, soup));

        Product p1 = new Product();
        ReflectionTestUtils.setField(p1, "id", "p1");
        p1.setName("Pizza");
        p1.setStock(2);
        p1.setAvailable(true);
        Product p2 = new Product();
        ReflectionTestUtils.setField(p2, "id", "p2");
        p2.setName("Soup");
        p2.setStock(0);
        p2.setAvailable(false);

        when(orderRepository.findById("o1")).thenReturn(Optional.of(past));
        when(cartRepository.findByUserEmail("a@b.com")).thenReturn(Optional.empty());
        when(productRepository.findById("p1")).thenReturn(Optional.of(p1));
        when(productRepository.findById("p2")).thenReturn(Optional.of(p2));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        java.util.Map<String, Object> result = orderService.reorder("a@b.com", "o1");

        assertEquals(List.of("Pizza"), result.get("added"));
        assertEquals(List.of("Soup"), result.get("skipped"));
        assertEquals(2, ((Cart) result.get("cart")).getItems().get(0).getQuantity());
    }
}
