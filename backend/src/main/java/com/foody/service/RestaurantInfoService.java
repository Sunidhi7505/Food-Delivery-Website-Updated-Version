package com.foody.service;

import com.foody.model.RestaurantInfo;
import com.foody.repository.RestaurantInfoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RestaurantInfoService {

    private static final Pattern HOURS = Pattern.compile(
            "(\\d{1,2}(?::\\d{2})?\\s*[AaPp][Mm])\\s*[-–to]+\\s*(\\d{1,2}(?::\\d{2})?\\s*[AaPp][Mm])");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h[:mm] a", Locale.ENGLISH);

    private final RestaurantInfoRepository restaurantInfoRepository;

    @Value("${foody.timezone:Asia/Kolkata}")
    private String timezone;

    @Value("${foody.restaurant.enforce-hours:true}")
    private boolean enforceHours;

    public RestaurantInfoService(RestaurantInfoRepository restaurantInfoRepository) {
        this.restaurantInfoRepository = restaurantInfoRepository;
    }

    public RestaurantInfo getRestaurant() {
        return restaurantInfoRepository.findAll()
                .stream()
                .findFirst()
                .orElseGet(() -> restaurantInfoRepository.save(new RestaurantInfo()));
    }

    public RestaurantInfo updateRestaurant(RestaurantInfo updated) {
        RestaurantInfo restaurant = getRestaurant();
        restaurant.setName(cleanOrDefault(updated.getName(), "Foody Kitchen"));
        restaurant.setDescription(cleanOrDefault(updated.getDescription(), "Fresh meals prepared close to you."));
        restaurant.setPhone(cleanOrDefault(updated.getPhone(), ""));
        restaurant.setAddressLine(cleanOrDefault(updated.getAddressLine(), ""));
        restaurant.setCity(cleanOrDefault(updated.getCity(), ""));
        restaurant.setOpeningHours(cleanOrDefault(updated.getOpeningHours(), ""));
        restaurant.setLatitude(updated.getLatitude());
        restaurant.setLongitude(updated.getLongitude());
        return restaurantInfoRepository.save(restaurant);
    }

    private String cleanOrDefault(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    public boolean isOpenNow() {
        return isOpenAt(getRestaurant().getOpeningHours(), LocalTime.now(ZoneId.of(timezone)));
    }

    public Map<String, Object> getStatus() {
        RestaurantInfo restaurant = getRestaurant();
        boolean open = isOpenNow();
        return Map.of(
                "open", open,
                "openingHours", restaurant.getOpeningHours() == null ? "" : restaurant.getOpeningHours(),
                "message", open ? "We are open - order now" : "We are closed right now"
        );
    }

    /** Parses hours like "10:00 AM - 11:00 PM" (overnight ranges supported). Unparseable hours mean always open. */
    boolean isOpenAt(String openingHours, LocalTime now) {
        if (!enforceHours || openingHours == null) {
            return true;
        }
        Matcher matcher = HOURS.matcher(openingHours);
        if (!matcher.find()) {
            return true;
        }
        try {
            LocalTime open = LocalTime.parse(normalize(matcher.group(1)), TIME);
            LocalTime close = LocalTime.parse(normalize(matcher.group(2)), TIME);
            if (open.equals(close)) {
                return true;
            }
            return open.isBefore(close)
                    ? !now.isBefore(open) && now.isBefore(close)
                    : !now.isBefore(open) || now.isBefore(close);
        } catch (RuntimeException ex) {
            return true;
        }
    }

    private String normalize(String time) {
        return time.trim().toUpperCase(Locale.ENGLISH).replaceAll("\\s+", " ").replaceAll("(?<=\\d)(AM|PM)", " $1");
    }
}
