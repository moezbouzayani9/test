package com.valuephone.image.management.images;

import com.valuephone.image.exception.ValuePhoneException;
import com.valuephone.image.management.share.images.ImageConfiguration;
import com.valuephone.image.management.share.images.ImageType;

public interface ImageConfigurationManager {
    ImageConfiguration getImageConfiguration(ImageType imageType) throws ValuePhoneException;
}
