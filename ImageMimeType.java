package com.valuephone.image.dto;

import com.valuephone.image.exception.ImageTypeNotSupportedException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Class MimeType
 *
 * @author Tomas Cigler
 */
public enum ImageMimeType {

    PNG("image/png", "image/x-png"),
    JPG("image/jpeg", "image/jpg", "image/pjpeg"),
    BMP("image/bmp", " image/x-windows-bmp");

    private final static Pattern PNG_PATTERN = Pattern.compile("png", Pattern.CASE_INSENSITIVE);
    private final static Pattern JPG_PATTERN = Pattern.compile("jpe?g", Pattern.CASE_INSENSITIVE);
    private final static Pattern GIF_PATTERN = Pattern.compile("gif", Pattern.CASE_INSENSITIVE);
    private final static Pattern BMP_PATTERN = Pattern.compile("bmp", Pattern.CASE_INSENSITIVE);

    private final Set<String> allowedTypes;
    private final String mainType;

    /**
     * @param mainType              One standard mime type string of file type, that will be chosen when showing image
     * @param optionalAcceptedTypes
     */
    ImageMimeType(String mainType, String... optionalAcceptedTypes) {
        if (mainType == null || mainType.isEmpty()) {
            throw new IllegalArgumentException("At least one mime type string has to be defined!");
        }
        this.mainType = mainType;
        this.allowedTypes = new HashSet<>();
        if (optionalAcceptedTypes != null && optionalAcceptedTypes.length > 0) {
            this.allowedTypes.addAll(Arrays.asList(optionalAcceptedTypes));
        }
    }

    /**
     * @return is the format lossless
     */
    public boolean isLossless() {
        // TODO: hack! No other lossy formats are currently supported - need to be rewritten for GIF etc.
        return !this.equals(JPG);
    }

    /**
     * @return enum entry lower-case name (corresponding to most common extension)
     */
    public String getExtension() {
        return this.name().toLowerCase();
    }

    /**
     * Gets mime type entry
     *
     * @param formatString
     * @return
     * @throws ImageTypeNotSupportedException
     */
    public static ImageMimeType fromFormatString(String formatString) throws ImageTypeNotSupportedException {

        if (PNG_PATTERN.matcher(formatString).matches()) {
            return PNG;
        }
        if (JPG_PATTERN.matcher(formatString).matches()) {
            return JPG;
        }
        if (BMP_PATTERN.matcher(formatString).matches()) {
            return BMP;
        }

        throw new ImageTypeNotSupportedException("Not known type " + formatString);
    }

    public static ImageMimeType fromString(String typeString) throws ImageTypeNotSupportedException {
        if (typeString != null) {
            final String typeLowerCase = typeString.toLowerCase(Locale.ENGLISH);
            for (ImageMimeType type : values()) {
                if (type.mainType.equals(typeLowerCase) || type.allowedTypes.contains(typeLowerCase)) {
                    return type;
                }
            }
        }
        throw new ImageTypeNotSupportedException(typeString);
    }

    public static boolean isValid(String typeString) {
        try {
            return fromString(typeString) != null;
        } catch (ImageTypeNotSupportedException ex) {
            return false;
        }
    }

    /**
     * @return mime type for image output
     */
    @Override
    public String toString() {
        return this.mainType;
    }

}
