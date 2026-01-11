package com.eshop.app.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ImageStorageFactory {

    private final CloudinaryStorageService cloudinaryService;
    private final BunnyNetStorageService bunnyService;

    @Value("${image.storage.provider:cloudinary}")
    private String provider;

    public ImageStorageFactory(CloudinaryStorageService cloudinaryService, BunnyNetStorageService bunnyService) {
        this.cloudinaryService = cloudinaryService;
        this.bunnyService = bunnyService;
    }

    public ImageStorageService get() {
        if (provider == null) return cloudinaryService;
        switch (provider.toLowerCase()) {
            case "bunny":
            case "bunny.net":
            case "bunnycdn":
                return bunnyService;
            case "cloudinary":
            default:
                return cloudinaryService;
        }
    }
}
