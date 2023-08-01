package com.valuephone.image.management.share.images;

public enum ImageType implements ImageConfiguration {

    APP_BANNER(ImagePoolType.APP_BANNER_IMAGE),
    COUPON(ImagePoolType.COUPON_IMAGE),
    LOYALTY_PARTNERSHIP_EXCHANGE_VOUCHER(ImagePoolType.LOYALTY_PARTNERSHIP_EXCHANGE_VOUCHER_IMAGE),
    LOYALTY_PROVIDER_IMAGE(ImagePoolType.LOYALTY_PROVIDER_IMAGE),
    NEWSLETTER_IMAGE(ImagePoolType.NEWSLETTER_IMAGE),
    STAMPCARD_BANNER(ImagePoolType.STAMPCARD_BANNER_IMAGE),
    STAMPCARD_STAMP(ImagePoolType.STAMPCARD_STAMP_IMAGE),
    STORE_IMAGE(ImagePoolType.RETAILER_SHOP_IMAGE),
    STORE_MAP_IMAGE(ImagePoolType.RETAILER_SHOP_MAP_IMAGE),
    USER_AVATAR(ImagePoolType.USER_AVATAR),
    RETAILER_IMAGE(ImagePoolType.RETAILER_IMAGE),
    RECIPE_IMAGE(ImagePoolType.RECIPE_IMAGE),

    // not used in native yet - default size definition:

    GIFT_CERTIFICATE_IMAGE(ImagePoolType.GIFT_CERTIFICATE_IMAGE),
    LINK_STRUCTURE_IMAGE(ImagePoolType.LINK_STRUCTURE_IMAGE),
    RETAILER_FLYER_IMAGE(ImagePoolType.RETAILER_FLYER_IMAGE),
    RETAILER_OFFER_GROUP_IMAGE(ImagePoolType.RETAILER_OFFER_GROUP_IMAGE),
    RETAILER_OFFER_IMAGE(ImagePoolType.RETAILER_OFFER_IMAGE),
    RETAILER_TEASER_IMAGE(ImagePoolType.RETAILER_TEASER_IMAGE),
    SYMBOL_SET_IMAGE(ImagePoolType.SYMBOL_SET_IMAGE),
    USER_IMAGE(ImagePoolType.USER_IMAGE),

    // bridge:
    ITEM(null), // EAN code images currently do not have a counter part on appServer
    DEFAULT(null),

    // null & empty types:
    UNKNOWN(ImagePoolType.UNKNOWN);

    private final ImagePoolType imagePoolType;

    ImageType(ImagePoolType imagePoolType) {
        this.imagePoolType = imagePoolType;
    }

    public ImagePoolType getImagePoolType() {
        return imagePoolType;
    }

    public int getMinWidth() {
        return imagePoolType == null ? 0 : imagePoolType.getMinWidth();
    }

    public int getMinHeight() {
        return imagePoolType == null ? 0 : imagePoolType.getMinHeight();
    }

    public int getMaxWidth() {
        return imagePoolType == null ? ImagePoolType.getDefaultMaxWidth() : imagePoolType.getMaxWidth();
    }

    public int getMaxHeight() {
        return imagePoolType == null ? ImagePoolType.getDefaultMaxHeight() : imagePoolType.getMaxHeight();
    }

    public int getMaxFileSize() {
        return imagePoolType == null ? ImagePoolType.getDefaultMaxFileSize() : imagePoolType.getMaxFileSize();
    }
}
