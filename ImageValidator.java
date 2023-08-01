package com.valuephone.image.management.images;

import com.valuephone.image.exception.ImageFileSizeException;
import com.valuephone.image.exception.ValuePhoneException;
import com.valuephone.image.management.share.images.ImageConfiguration;
import com.valuephone.image.management.share.images.ImageType;

public interface ImageValidator {

    void validateImageFileSize(byte[] imageData, ImageType imageType) throws ValuePhoneException;

    void validateImageFileSize(byte[] imageData, ImageConfiguration imageConfiguration) throws ImageFileSizeException;
}
