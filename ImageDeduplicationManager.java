package com.valuephone.image.management.images;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.valuephone.image.exception.ValuePhoneException;
import com.valuephone.image.helper.Reject;
import com.valuephone.image.management.share.images.ImageType;

/**
 * Created with IntelliJ IDEA.
 * <p>
 * User: ahack Date: 18.03.16 Time: 11:48
 */
public interface ImageDeduplicationManager {

    /**
     * method to create the HashCode object for an image from its binary data
     *
     * @param imageData the binary data of the image
     * @return the HashCode object for this image
     */
    static HashCode createAndReturnHashCodeForImageData(byte[] imageData) {
        Reject.ifNull(imageData, "no image data");
        Reject.ifTrue(imageData.length == 0, "empty image data");

        return Hashing.md5().hashBytes(imageData);
    }

    /**
     * method to create the DeduplicationKey for an existing image by its key values
     *
     * @param imageSize     the total size of this image in [bytes]
     * @param imageHashCode the HashCode object for this image
     * @param imageType     the ImageType for this image
     * @return the DeduplicationKey for this image
     */
    static ImageDeduplicationKey createAndReturnDeduplicationKeyForImageSizeAndHashCodeAndType(
            int imageSize, HashCode imageHashCode, ImageType imageType) {
        Reject.ifTrue(imageSize <= 0, "wrong image size");
        Reject.ifNull(imageHashCode, "wrong image hash");
        imageType = imageType == null ? ImageType.UNKNOWN : imageType;

        return new ImageDeduplicationKey(imageSize, imageHashCode, imageType);
    }

    /**
     * method to create the DeduplicationKey for an image from its name and binary data
     *
     * @param imageName the original name of this image
     * @param imageData the binary data of the image
     * @return the DeduplicationKey for this image
     * @param imageType the ImageType for this image
     */
    static ImageDeduplicationKey createAndReturnDeduplicationKeyForImageNameAndImageData(
            String imageName, byte[] imageData, ImageType imageType) {
        Reject.ifNull(imageName, "no image name");
        Reject.ifNull(imageData, "no image data");
        Reject.ifTrue(imageData.length == 0, "empty image data");
        imageType = imageType == null ? ImageType.UNKNOWN : imageType;

        return createAndReturnDeduplicationKeyForImageSizeAndHashCodeAndType(
                imageData.length, createAndReturnHashCodeForImageData(imageData), imageType);
    }

    /**
     * method to create the LifetimeKey for an existing image by its key values
     *
     * @param deduplicatedImageId the ID of the duplicated (the original) image
     * @return the LifetimeKey for this image
     */
    static ImageLifetimeKey createAndReturnLifetimeKeyForDeduplicatedImageIdAndType(
            Long deduplicatedImageId) {
        Reject.ifNull(deduplicatedImageId, "no deduplicated image id");

        return new ImageLifetimeKey(deduplicatedImageId);
    }

    /**
     * method to return the ID of the duplicated (the original) image if one exists This method is designed for the
     * deduplication procedure. It is designed to be called from image manager during creation of image entities.
     *
     * @param deduplicationKey the DeduplicationKey of the image
     * @return the ID of the duplicated image if exists or 0 if non exists
     */
    long getIdForImageDeduplicationKey(
            ImageDeduplicationKey deduplicationKey) throws ValuePhoneException;

    /**
     * method to remove an cached entry for the given BoImageDeduplicationKey. If an lifetime is present for this keys
     * image, then the image lifetime will be removed of the cache to.
     *
     * @param imageDeduplicationKey the DeduplicationKey of the image
     */
    void removeCachedEntryForDeduplicationKey(ImageDeduplicationKey imageDeduplicationKey);

    /**
     * method to return the ImageLifetime of the duplicated (the original) image if one exists
     *
     * @param imageLifetimeKey the LifetimeKey for this image
     * @return the BoImageLifetime of the requested image.
     */
    ImageLifetime getLifetimeForImageLifetimeKey(ImageLifetimeKey imageLifetimeKey) throws ValuePhoneException;

    /**
     * method to manually cache an duplicated (the original) images lifetime
     *
     * @param imageLifetime    the lifetime of the duplicated image
     * @param imageLifetimeKey the LifetimeKey of the image
     * @return the previous value associated with the specified boImageLifetimeKey, or {@code null} if there was no
     * mapping for the key
     */
    ImageLifetime cacheLifetimeForImageLifetimeKeyIfAbsent(
            ImageLifetime imageLifetime, ImageLifetimeKey imageLifetimeKey) throws ValuePhoneException;

    /**
     * method to manually replace an duplicated (the original) images lifetime
     *
     * @param expectedImageLifetime the expected currently cached lifetime of the duplicated image
     * @param newImageLifetime      the new to be cached lifetime of the duplicated image
     * @param imageLifetimeKey      the LifetimeKey of the image
     * @return {@code true} if currently cached value matched expected cached value {@code false} if currently cached
     * value did not match expected cached value
     */
    boolean replaceLifetimeForLifetimeKey(
            ImageLifetime expectedImageLifetime, ImageLifetime newImageLifetime,
            ImageLifetimeKey imageLifetimeKey) throws ValuePhoneException;

    /**
     * method to remove the ImageLifetime of the duplicated (the original) image
     *
     * @param imageLifetimeKey the LifetimeKey for this image
     */
    void removeCachedEntryForImageLifetimeKey(ImageLifetimeKey imageLifetimeKey);
}
