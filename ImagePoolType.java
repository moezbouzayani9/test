package com.valuephone.image.management.share.images;

/**
 * @author lbalint
 * @version 03.01.00
 */
public enum ImagePoolType {

    APP_BANNER_IMAGE(0, 0, 1920, 1280),
    COUPON_IMAGE(50, 320, 2000, 2000),
    GIFT_CERTIFICATE_IMAGE(),
    LINK_STRUCTURE_IMAGE(),
    LOYALTY_PARTNERSHIP_EXCHANGE_VOUCHER_IMAGE(200, 200, 1000, 1000),
    LOYALTY_PROVIDER_IMAGE(0, 0, 512, 512),
    NEWSLETTER_IMAGE(0, 0, 2000, 2000),
    RECIPE_IMAGE(),
    RETAILER_FLYER_IMAGE(),
    RETAILER_IMAGE(),
    RETAILER_OFFER_GROUP_IMAGE(),
    RETAILER_OFFER_IMAGE(0, 0, 2000, 2000),
    RETAILER_SHOP_IMAGE(50, 320, 2000, 2000),
    RETAILER_SHOP_LOGO_IMAGE(50, 320, 2000, 2000),
    RETAILER_SHOP_MAP_IMAGE(50, 320, 2000, 2000),
    RETAILER_TEASER_IMAGE(1000, 1000, 2000, 2000),
    STAMPCARD_BANNER_IMAGE(0, 0, 2000, 2000),
    STAMPCARD_STAMP_IMAGE(0, 0, 512, 512),
    SYMBOL_SET_IMAGE(),
    USER_AVATAR(0, 0, 600, 600),
    USER_IMAGE(),
    UNKNOWN();

    private final int minWidth;
    private final int minHeight;
    private final int maxWidth;
    private final int maxHeight;
    //  size of the file in KB
    private final int maxFileSize;

    ImagePoolType() {
        // no dimensions defined yet, using default (12 Mpx):
        this.minWidth = 0;
        this.minHeight = 0;
        this.maxWidth = getDefaultMaxWidth();
        this.maxHeight = getDefaultMaxHeight();
        this.maxFileSize = getDefaultMaxFileSize();
    }


    ImagePoolType(int minWidth, int minHeight, int maxWidth, int maxHeight) {
        this.minWidth = minWidth;
        this.minHeight = minHeight;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.maxFileSize = getDefaultMaxFileSize();
    }

    ImagePoolType(int minWidth, int minHeight, int maxWidth, int maxHeight, int maxFileSize) {
        this.minWidth = minWidth;
        this.minHeight = minHeight;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.maxFileSize = maxFileSize;
    }

    public static int getDefaultMaxWidth() {
        return 4240;
    }

    public static int getDefaultMaxHeight() {
        return 2824;
    }

    public static int getDefaultMaxFileSize() {
        return 1024;
    }

    public int getMinWidth() {
        return minWidth;
    }

    public int getMinHeight() {
        return minHeight;
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public int getMaxFileSize() {
        return maxFileSize;
    }
}
