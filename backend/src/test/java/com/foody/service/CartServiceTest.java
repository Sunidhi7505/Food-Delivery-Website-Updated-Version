package com.foody.service;

import com.foody.model.Cart;
import com.foody.model.Product;
import com.foody.repository.CartRepository;
import com.foody.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock CartRepository cartRepository;
    @Mock ProductRepository productRepository;
    @InjectMocks CartService cartService;

    private Product product(int stock, boolean available) {
        Product p = new Product();
        ReflectionTestUtils.setField(p, "id", "p1");
        p.setName("Pizza");
        p.setPrice(150);
        p.setStock(stock);
        p.setAvailable(available);
        return p;
    }

    @Test
    void addToCartCreatesCartWithItem() {
        when(productRepository.findById("p1")).thenReturn(Optional.of(product(10, true)));
        when(cartRepository.findByUserEmail("a@b.com")).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        Cart cart = cartService.addToCart("a@b.com", "p1", 2);

        assertEquals(1, cart.getItems().size());
        assertEquals(2, cart.getItems().get(0).getQuantity());
        assertEquals("Pizza", cart.getItems().get(0).getName());
    }

    @Test
    void addToCartRejectsNonPositiveQuantity() {
        assertThrows(RuntimeException.class, () -> cartService.addToCart("a@b.com", "p1", 0));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void addToCartRejectsOutOfStockProduct() {
        when(productRepository.findById("p1")).thenReturn(Optional.of(product(0, false)));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> cartService.addToCart("a@b.com", "p1", 1));

        assertEquals("Pizza is out of stock", ex.getMessage());
    }

    @Test
    void addToCartRejectsQuantityAboveStock() {
        when(productRepository.findById("p1")).thenReturn(Optional.of(product(3, true)));
        when(cartRepository.findByUserEmail("a@b.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> cartService.addToCart("a@b.com", "p1", 5));

        assertEquals("Only 3 item(s) available for Pizza", ex.getMessage());
    }
}
