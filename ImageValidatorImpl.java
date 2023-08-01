package com.valuephone.image.management.images;

import com.valuephone.image.exception.ImageFileSizeException;
import com.valuephone.image.exception.ValuePhoneException;
import com.valuephone.image.management.share.images.ImageConfiguration;
import com.valuephone.image.management.share.images.ImageType;

public class ImageValidatorImpl implements ImageValidator {

    ImageConfigurationManager imageConfigurationManager;

    public ImageValidatorImpl(ImageConfigurationManager imageConfigurationManager) {
        this.imageConfigurationManager = imageConfigurationManager;
    }

    private int kilobytesToBytes(int kilobytes) {
        return kilobytes * 1000;
    }

    public void validateImageFileSize(byte[] imageData, ImageConfiguration imageConfiguration) throws ImageFileSizeException {
        if (imageData.length > kilobytesToBytes(imageConfiguration.getMaxFileSize())) {
            throw new ImageFileSizeException("Image size is bigger than" + imageConfiguration.getMaxFileSize() + " kB");
        }
    }

    @Override
    public void validateImageFileSize(byte[] imageData, ImageType imageType) throws ValuePhoneException {
        ImageConfiguration imageConfiguration = imageConfigurationManager.getImageConfiguration(imageType);
        validateImageFileSize(imageData, imageConfiguration);
    }
}
