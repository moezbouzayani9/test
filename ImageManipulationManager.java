package com.valuephone.image.helper;

/**
 * @author tcigler
 * @since 1.0
 */
public class ImageManipulationManager {

    private ImageManipulationManager() {
    }

    /**
     * Change size of imSize proportionaly depending on required width and height
     *
     * @param imSize         size wḧich could be changed
     * @param requiredWidth  required width
     * @param requiredHeight required height
     * @return if required size affect image size
     */
    public static boolean resize(ImageSize imSize, Integer requiredWidth, Integer requiredHeight) {
        int imWidth = imSize.imWidth;
        int imHeight = imSize.imHeight;

        int reqWidth = (requiredWidth == null || requiredWidth == 0) ? imWidth : Math.min(imWidth, requiredWidth);
        int reqHeight = (requiredHeight == null || requiredHeight == 0) ? imHeight : Math.min(imHeight, requiredHeight);

        double ratio = Math.min(reqWidth / (double) imWidth, reqHeight / (double) imHeight);

        if (ratio <= 0) {
            throw new IllegalArgumentException("Image size " + reqWidth + "x" + reqHeight + " cannot be negative or zero");
        } else if (ratio < 1) {
            imSize.imHeight = ((int) Math.round(imHeight * ratio));
            imSize.imWidth = ((int) Math.round(imWidth * ratio));
            return true;
        } else {
            return false;
        }

    }

}
