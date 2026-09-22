package com.trichyestates.estatehub.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Builds the display label ("₹78 Lakhs", "₹1.35 Crore") the UI shows on property cards. */
public final class PriceFormatter {

    private static final long LAKH = 100_000L;
    private static final long CRORE = 10_000_000L;

    private PriceFormatter() { }

    public static String label(long price) {
        if (price >= CRORE) {
            return "₹" + scaled(price, CRORE) + " Crore";
        }
        if (price >= LAKH) {
            String value = scaled(price, LAKH);
            return "₹" + value + ("1".equals(value) ? " Lakh" : " Lakhs");
        }
        return "₹" + String.format("%,d", price);
    }

    private static String scaled(long price, long unit) {
        return BigDecimal.valueOf(price)
                .divide(BigDecimal.valueOf(unit), 2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }
}
