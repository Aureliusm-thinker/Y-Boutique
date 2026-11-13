package com.yvette.api.checkout;

import com.yvette.api.order.Order;
import com.yvette.api.order.OrderDtos.PaymentView;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public final class CheckoutDtos {

    private CheckoutDtos() {}

    public record CheckoutRequest(
            @NotNull UUID cartToken,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 20) String phone,
            @NotBlank @Size(max = 120) String fullName,
            @Size(max = 200) String addressLine1,
            @Size(max = 200) String addressLine2,
            @Size(max = 80) String city,
            @Size(max = 40) String county,
            @Size(max = 20) String postalCode,
            @Size(max = 300) String deliveryNotes,
            @NotBlank String shippingMethod,
            @NotNull Order.PaymentMethod paymentMethod,
            /** M-Pesa number to prompt, if different from the contact phone. */
            @Size(max = 20) String mpesaPhone) {}

    public record CheckoutResult(String orderNumber, String accessToken, PaymentView payment) {}

    public record RetryPaymentRequest(@NotNull UUID token, @Size(max = 20) String mpesaPhone) {}

    public record TestCardRequest(@NotNull UUID token, @NotBlank @Size(max = 30) String cardNumber) {}

    public record TestMpesaRequest(@NotNull UUID token, boolean approve) {}

    public record CheckoutOptions(List<String> paymentMethods, boolean testMode, List<String> counties) {}

    /** The 47 counties, for the address form. */
    public static final List<String> COUNTIES = List.of(
            "Baringo", "Bomet", "Bungoma", "Busia", "Elgeyo-Marakwet", "Embu", "Garissa", "Homa Bay", "Isiolo",
            "Kajiado", "Kakamega", "Kericho", "Kiambu", "Kilifi", "Kirinyaga", "Kisii", "Kisumu", "Kitui", "Kwale",
            "Laikipia", "Lamu", "Machakos", "Makueni", "Mandera", "Marsabit", "Meru", "Migori", "Mombasa",
            "Murang'a", "Nairobi", "Nakuru", "Nandi", "Narok", "Nyamira", "Nyandarua", "Nyeri", "Samburu", "Siaya",
            "Taita-Taveta", "Tana River", "Tharaka-Nithi", "Trans Nzoia", "Turkana", "Uasin Gishu", "Vihiga",
            "Wajir", "West Pokot");
}
