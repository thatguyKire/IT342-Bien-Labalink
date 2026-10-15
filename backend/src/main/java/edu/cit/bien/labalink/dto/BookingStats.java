package edu.cit.bien.labalink.dto;

import java.math.BigDecimal;

public record BookingStats(
        long totalBookings,
        long activeBookings,
        long pendingBookings,
        BigDecimal totalRevenue) {}
