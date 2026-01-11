package com.eshop.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.eshop.app.constants.ApiConstants;

import java.math.BigDecimal;
import java.util.*;

/**
 * Payment Methods Controller
 * Provides information about available payment methods, card validation, and UPI services
 */
@Tag(name = "Payment Methods", description = "Payment method discovery and validation")
@RestController
@RequestMapping(ApiConstants.Endpoints.PAYMENT_METHODS)
@RequiredArgsConstructor
@SecurityRequirement(name = "Keycloak OAuth2")
@SecurityRequirement(name = "Bearer Authentication")
public class PaymentMethodsController {
    
    @Value("${upi.enabled:true}")
    private boolean upiEnabled;
    
    @Value("${emi.enabled:true}")
    private boolean emiEnabled;
    
    @Value("${emi.min.amount:5000}")
    private BigDecimal emiMinAmount;
    
    @GetMapping("/available")
    @Operation(summary = "Get Available Payment Methods", 
               description = "Get all available payment methods based on user location and order amount")
    @ApiResponse(responseCode = "200", description = "List of available payment methods")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAvailablePaymentMethods(
            @Parameter(description = "Order amount") @RequestParam(required = false) BigDecimal amount,
            @Parameter(description = "Currency code") @RequestParam(defaultValue = "INR") String currency,
            @Parameter(description = "Country code") @RequestParam(defaultValue = "IN") String country) {
        
        Map<String, Object> response = new HashMap<>();
        
        // Credit/Debit Cards
        List<Map<String, Object>> cardMethods = new ArrayList<>();
        
        if ("IN".equals(country)) {
            // Indian Cards
            cardMethods.add(createPaymentMethod("CREDIT_CARD_VISA", "Visa Credit Card", "card", true, null));
            cardMethods.add(createPaymentMethod("CREDIT_CARD_MASTERCARD", "Mastercard Credit", "card", true, null));
            cardMethods.add(createPaymentMethod("CREDIT_CARD_RUPAY", "RuPay Credit Card", "card", true, null));
            cardMethods.add(createPaymentMethod("DEBIT_CARD_VISA", "Visa Debit Card", "card", true, null));
            cardMethods.add(createPaymentMethod("DEBIT_CARD_MASTERCARD", "Mastercard Debit", "card", true, null));
            cardMethods.add(createPaymentMethod("DEBIT_CARD_RUPAY", "RuPay Debit Card", "card", true, null));
        } else {
            // International Cards
            cardMethods.add(createPaymentMethod("CREDIT_CARD_VISA", "Visa Credit Card", "card", true, null));
            cardMethods.add(createPaymentMethod("CREDIT_CARD_MASTERCARD", "Mastercard Credit", "card", true, null));
            cardMethods.add(createPaymentMethod("CREDIT_CARD_AMERICAN_EXPRESS", "American Express", "card", true, null));
            cardMethods.add(createPaymentMethod("DEBIT_CARD_VISA", "Visa Debit Card", "card", true, null));
            cardMethods.add(createPaymentMethod("DEBIT_CARD_MASTERCARD", "Mastercard Debit", "card", true, null));
        }
        
        // UPI Methods (India only)
        List<Map<String, Object>> upiMethods = new ArrayList<>();
        if ("IN".equals(country) && upiEnabled) {
            upiMethods.add(createPaymentMethod("UPI", "UPI (Any App)", "upi", true, "Pay using any UPI app"));
            upiMethods.add(createPaymentMethod("UPI_GOOGLEPAY", "Google Pay", "upi", true, "googlepay://"));
            upiMethods.add(createPaymentMethod("UPI_PHONEPE", "PhonePe", "upi", true, "phonepe://"));
            upiMethods.add(createPaymentMethod("UPI_PAYTM", "Paytm UPI", "upi", true, "paytmmp://"));
            upiMethods.add(createPaymentMethod("UPI_AMAZON_PAY", "Amazon Pay", "upi", true, "amazonpay://"));
            upiMethods.add(createPaymentMethod("UPI_BHIM", "BHIM UPI", "upi", true, "bhim://"));
            upiMethods.add(createPaymentMethod("UPI_WHATSAPP", "WhatsApp Pay", "upi", true, "whatsapp://"));
        }
        
        // Digital Wallets
        List<Map<String, Object>> walletMethods = new ArrayList<>();
        if ("IN".equals(country)) {
            walletMethods.add(createPaymentMethod("PAYTM_WALLET", "Paytm Wallet", "wallet", true, null));
            walletMethods.add(createPaymentMethod("PHONEPE_WALLET", "PhonePe Wallet", "wallet", true, null));
            walletMethods.add(createPaymentMethod("AMAZON_PAY_WALLET", "Amazon Pay Wallet", "wallet", true, null));
            walletMethods.add(createPaymentMethod("MOBIKWIK", "MobiKwik Wallet", "wallet", true, null));
            walletMethods.add(createPaymentMethod("FREECHARGE", "FreeCharge Wallet", "wallet", true, null));
        }
        
        // International wallets
        walletMethods.add(createPaymentMethod("PAYPAL", "PayPal", "wallet", true, null));
        
        // Net Banking (India)
        List<Map<String, Object>> netBankingMethods = new ArrayList<>();
        if ("IN".equals(country)) {
            netBankingMethods.add(createPaymentMethod("NET_BANKING", "Net Banking", "netbanking", true, "All major banks supported"));
        }
        
        // EMI Options (if amount qualifies)
        List<Map<String, Object>> emiMethods = new ArrayList<>();
        if (emiEnabled && amount != null && amount.compareTo(emiMinAmount) >= 0) {
            emiMethods.add(createEmiMethod("EMI", "EMI (3-60 months)", "emi", true, 3, 60));
        }
        
        // Buy Now Pay Later
        List<Map<String, Object>> bnplMethods = new ArrayList<>();
        if (amount != null && amount.compareTo(BigDecimal.valueOf(1000)) >= 0) {
            bnplMethods.add(createPaymentMethod("BUY_NOW_PAY_LATER", "Buy Now Pay Later", "bnpl", true, "No interest for first 30 days"));
        }
        
        // Cash on Delivery
        List<Map<String, Object>> codMethods = new ArrayList<>();
        codMethods.add(createPaymentMethod("CASH_ON_DELIVERY", "Cash on Delivery", "cod", true, "Pay when you receive"));
        
        response.put("cards", cardMethods);
        response.put("upi", upiMethods);
        response.put("wallets", walletMethods);
        response.put("netbanking", netBankingMethods);
        response.put("emi", emiMethods);
        response.put("bnpl", bnplMethods);
        response.put("cod", codMethods);
        response.put("currency", currency);
        response.put("country", country);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/validate-card")
    @Operation(summary = "Validate Card Details", 
               description = "Validate credit/debit card number and detect card type")
    @ApiResponse(responseCode = "200", description = "Card validation result")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> validateCard(@RequestBody Map<String, String> cardData) {
        
        String cardNumber = cardData.get("cardNumber");
        Map<String, Object> result = new HashMap<>();
        
        if (cardNumber == null || cardNumber.trim().isEmpty()) {
            result.put("valid", false);
            result.put("error", "Card number is required");
            return ResponseEntity.ok(result);
        }
        
        // Remove spaces and non-digits
        String cleanCardNumber = cardNumber.replaceAll("\\D", "");
        
        // Basic validation
        if (cleanCardNumber.length() < 13 || cleanCardNumber.length() > 19) {
            result.put("valid", false);
            result.put("error", "Invalid card number length");
            return ResponseEntity.ok(result);
        }
        
        // Luhn algorithm validation
        boolean luhnValid = isValidLuhn(cleanCardNumber);
        
        if (!luhnValid) {
            result.put("valid", false);
            result.put("error", "Invalid card number");
            return ResponseEntity.ok(result);
        }
        
        // Detect card brand
        String cardBrand = detectCardBrand(cleanCardNumber);
        String cardType = detectCardType(cleanCardNumber);
        
        result.put("valid", true);
        result.put("cardBrand", cardBrand);
        result.put("cardType", cardType);
        result.put("lastFour", cleanCardNumber.substring(cleanCardNumber.length() - 4));
        result.put("supportedGateways", getSupportedGateways(cardBrand));
        
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/validate-upi")
    @Operation(summary = "Validate UPI ID", 
               description = "Validate UPI Virtual Payment Address (VPA)")
    @ApiResponse(responseCode = "200", description = "UPI validation result")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> validateUpi(@RequestBody Map<String, String> upiData) {
        
        String upiId = upiData.get("upiId");
        Map<String, Object> result = new HashMap<>();
        
        if (upiId == null || upiId.trim().isEmpty()) {
            result.put("valid", false);
            result.put("error", "UPI ID is required");
            return ResponseEntity.ok(result);
        }
        
        // UPI ID format validation
        String upiPattern = "^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+$";
        boolean formatValid = upiId.matches(upiPattern);
        
        if (!formatValid) {
            result.put("valid", false);
            result.put("error", "Invalid UPI ID format");
            return ResponseEntity.ok(result);
        }
        
        // Extract UPI provider from handle
        String[] parts = upiId.split("@");
        String handle = parts.length > 1 ? parts[1] : "";
        String provider = getUpiProvider(handle);
        
        result.put("valid", true);
        result.put("provider", provider);
        result.put("handle", handle);
        result.put("supportedApps", getSupportedUpiApps());
        
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/gateways")
    @Operation(summary = "Get Payment Gateways", 
               description = "Get available payment gateways and their capabilities")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getPaymentGateways() {
        
        Map<String, Object> gateways = new HashMap<>();
        
        // Stripe
        Map<String, Object> stripe = new HashMap<>();
        stripe.put("name", "Stripe");
        stripe.put("region", "Global");
        stripe.put("supportedMethods", Arrays.asList("CREDIT_CARD", "DEBIT_CARD"));
        stripe.put("currencies", Arrays.asList("USD", "EUR", "GBP", "INR"));
        stripe.put("fees", "2.9% + $0.30");
        gateways.put("STRIPE", stripe);
        
        // Razorpay
        Map<String, Object> razorpay = new HashMap<>();
        razorpay.put("name", "Razorpay");
        razorpay.put("region", "India");
        razorpay.put("supportedMethods", Arrays.asList("CREDIT_CARD", "DEBIT_CARD", "UPI", "WALLETS", "NET_BANKING"));
        razorpay.put("currencies", Arrays.asList("INR"));
        razorpay.put("fees", "2% + GST");
        gateways.put("RAZORPAY", razorpay);
        
        // PayU
        Map<String, Object> payu = new HashMap<>();
        payu.put("name", "PayU");
        payu.put("region", "India");
        payu.put("supportedMethods", Arrays.asList("CREDIT_CARD", "DEBIT_CARD", "UPI", "NET_BANKING"));
        payu.put("currencies", Arrays.asList("INR"));
        payu.put("fees", "2.4% + GST");
        gateways.put("PAYU", payu);
        
        // Cashfree
        Map<String, Object> cashfree = new HashMap<>();
        cashfree.put("name", "Cashfree");
        cashfree.put("region", "India");
        cashfree.put("supportedMethods", Arrays.asList("CREDIT_CARD", "DEBIT_CARD", "UPI", "WALLETS", "EMI"));
        cashfree.put("currencies", Arrays.asList("INR"));
        cashfree.put("fees", "1.75% + GST");
        gateways.put("CASHFREE", cashfree);
        
        return ResponseEntity.ok(gateways);
    }
    
    // Helper Methods
    
    private Map<String, Object> createPaymentMethod(String code, String name, String type, boolean enabled, String description) {
        Map<String, Object> method = new HashMap<>();
        method.put("code", code);
        method.put("name", name);
        method.put("type", type);
        method.put("enabled", enabled);
        if (description != null) {
            method.put("description", description);
        }
        return method;
    }
    
    private Map<String, Object> createEmiMethod(String code, String name, String type, boolean enabled, int minTenure, int maxTenure) {
        Map<String, Object> method = createPaymentMethod(code, name, type, enabled, null);
        method.put("minTenure", minTenure);
        method.put("maxTenure", maxTenure);
        method.put("minAmount", emiMinAmount);
        return method;
    }
    
    private boolean isValidLuhn(String cardNumber) {
        int sum = 0;
        boolean alternate = false;
        
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));
            
            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit % 10) + 1;
                }
            }
            
            sum += digit;
            alternate = !alternate;
        }
        
        return sum % 10 == 0;
    }
    
    private String detectCardBrand(String cardNumber) {
        if (cardNumber.startsWith("4")) return "VISA";
        if (cardNumber.startsWith("5") || cardNumber.matches("^2[2-7].*")) return "MASTERCARD";
        if (cardNumber.matches("^3[47].*")) return "AMERICAN_EXPRESS";
        if (cardNumber.startsWith("6")) return "DISCOVER";
        if (cardNumber.matches("^(508|60).*")) return "RUPAY";
        return "UNKNOWN";
    }
    
    private String detectCardType(String cardNumber) {
        // Simplified card type detection - in real implementation, use BIN database
        return "CREDIT"; // Default to credit
    }
    
    private List<String> getSupportedGateways(String cardBrand) {
        return switch (cardBrand) {
            case "RUPAY" -> Arrays.asList("RAZORPAY", "PAYU", "CASHFREE");
            case "VISA", "MASTERCARD" -> Arrays.asList("STRIPE", "RAZORPAY", "PAYU", "CASHFREE");
            case "AMERICAN_EXPRESS" -> Arrays.asList("STRIPE");
            default -> Arrays.asList("STRIPE");
        };
    }
    
    private String getUpiProvider(String handle) {
        return switch (handle.toLowerCase()) {
            case "oksbi", "sbi" -> "State Bank of India";
            case "hdfcbank" -> "HDFC Bank";
            case "icici" -> "ICICI Bank";
            case "axisbank" -> "Axis Bank";
            case "paytm" -> "Paytm Payments Bank";
            case "ybl" -> "Yes Bank";
            default -> "Unknown Provider";
        };
    }
    
    private List<String> getSupportedUpiApps() {
        return Arrays.asList("Google Pay", "PhonePe", "Paytm", "BHIM", "Amazon Pay", "WhatsApp Pay");
    }
}