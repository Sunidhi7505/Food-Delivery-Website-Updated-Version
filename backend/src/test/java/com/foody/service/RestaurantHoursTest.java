package com.foody.service;

import com.foody.repository.RestaurantInfoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class RestaurantHoursTest {

    private RestaurantInfoService service(boolean enforce) {
        RestaurantInfoService s = new RestaurantInfoService(mock(RestaurantInfoRepository.class));
        ReflectionTestUtils.setField(s, "enforceHours", enforce);
        ReflectionTestUtils.setField(s, "timezone", "Asia/Kolkata");
        return s;
    }

    @Test
    void daytimeRange() {
        RestaurantInfoService s = service(true);
        assertTrue(s.isOpenAt("10:00 AM - 11:00 PM", LocalTime.of(12, 30)));
        assertFalse(s.isOpenAt("10:00 AM - 11:00 PM", LocalTime.of(23, 30)));
        assertFalse(s.isOpenAt("10:00 AM - 11:00 PM", LocalTime.of(9, 59)));
    }

    @Test
    void overnightRangeAndFallbacks() {
        RestaurantInfoService s = service(true);
        assertTrue(s.isOpenAt("6 PM - 2 AM", LocalTime.of(1, 0)));
        assertFalse(s.isOpenAt("6 PM - 2 AM", LocalTime.of(15, 0)));
        assertTrue(s.isOpenAt("Open all day", LocalTime.of(3, 0)));      // unparseable -> open
        assertTrue(service(false).isOpenAt("10:00 AM - 11:00 PM", LocalTime.of(3, 0))); // enforcement off
    }
}
