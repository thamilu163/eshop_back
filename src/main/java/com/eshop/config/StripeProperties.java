package com.eshop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Configuration
@ConfigurationProperties(prefix = "stripe")
@Validated
public class StripeProperties {

    @NotBlank(message = "Stripe secret key is required")
    @Pattern(regexp = "^sk_(test|live)_.*", message = "Invalid Stripe secret key format")
    private String secretKey;

    @NotBlank(message = "Stripe publishable key is required")
    @Pattern(regexp = "^pk_(test|live)_.*", message = "Invalid Stripe publishable key format")
    private String publishableKey;

    private String currency = "USD";

    private String apiVersion = "2023-10-16";

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getPublishableKey() {
        return publishableKey;
    }

    public void setPublishableKey(String publishableKey) {
        this.publishableKey = publishableKey;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }
}
