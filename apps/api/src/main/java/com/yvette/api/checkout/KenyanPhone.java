package com.yvette.api.checkout;

/** Kenyan mobile numbers in the 2547XXXXXXXX / 2541XXXXXXXX form M-Pesa expects. */
public final class KenyanPhone {

    private KenyanPhone() {}

    /** Accepts 0712 345 678, +254 712 345 678, 254712345678, 712345678 (and 01… numbers). Null if invalid. */
    public static String normalise(String input) {
        if (input == null) return null;
        String digits = input.replaceAll("[\\s\\-()+]", "");
        if (!digits.matches("\\d+")) return null;
        if (digits.startsWith("254")) digits = digits.substring(3);
        else if (digits.startsWith("0")) digits = digits.substring(1);
        if (!digits.matches("[17]\\d{8}")) return null;
        return "254" + digits;
    }
}
