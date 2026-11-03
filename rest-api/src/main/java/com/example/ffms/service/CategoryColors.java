package com.example.ffms.service;

import java.util.List;

/**
 * The canonical category palette. Clients render the stored value returned by the API
 * and never assign colors from their own list order.
 */
public final class CategoryColors {

    private static final List<String> PALETTE = List.of(
            "#7C5CFC",
            "#FF5C7A",
            "#3B82F6",
            "#F59E0B",
            "#A855F7",
            "#64748B",
            "#EC4899"
    );

    private CategoryColors() {
    }

    public static String forId(Long categoryId) {
        if (categoryId == null) {
            return PALETTE.get(0);
        }
        int index = Math.floorMod(categoryId - 1, PALETTE.size());
        return PALETTE.get(index);
    }
}
