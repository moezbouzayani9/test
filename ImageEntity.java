package com.valuephone.image.dto;

import com.valuephone.image.management.share.images.ImageType;
import lombok.ToString;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * @author dsimon
 */
@ToString
public class ImageEntity  {



    private long id;

    private Long deduplicatedImageId;

    private String name;

    private OffsetDateTime modificationDate = OffsetDateTime.now();

    private int teaserImageType = 0;

    private String mimeType;

    private OffsetDateTime validFrom;

    private OffsetDateTime validTill;

    private boolean deleted = false;

    private ImageType imageType;

    private Integer imageSize;

    private String imageHash;

    private LocalDate endOfLife;

    public ImageEntity() {}

    public void setup(ImageEntity fromImage) {
        this.setTeaserImageType(fromImage.getTeaserImageType());
        this.setModificationDate(fromImage.getModificationDate());
        this.setName(fromImage.getName());
        this.setMimeType(fromImage.getMimeType());
        this.setImageType(fromImage.getImageType());
        this.setDeduplicatedImageId(fromImage.getDeduplicatedImageId());
    }

    /**
     * @return the id
     */
    public long getId() {
        return id;
    }

    /**
     * @param id
     *            the id to set
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *            the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the eTag
     */
    public OffsetDateTime getModificationDate() {
        return modificationDate;
    }

    public void setModificationDate(OffsetDateTime modificationDate) {
        this.modificationDate = modificationDate;
    }

    /**
     * @param functionType
     *            the functionType to set
     */
    public void setTeaserImageType(int functionType) {
        this.teaserImageType = functionType;
    }

    /**
     * @return the functionType
     */
    public int getTeaserImageType() {
        return teaserImageType;
    }

    /**
     * @param mimeType
     *            the mimeType to set
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * @return the mimeType
     */
    public String getMimeType() {
        return mimeType;
    }

    public LocalDate getEndOfLife() {
        return endOfLife;
    }

    public void setEndOfLife(LocalDate endOfLife) {
        this.endOfLife = endOfLife;
    }

    public OffsetDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(OffsetDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public OffsetDateTime getValidTill() {
        return validTill;
    }

    public void setValidTill(OffsetDateTime validTill) {
        this.validTill = validTill;
    }

    public ImageType getImageType() {
        return imageType;
    }

    public void setImageType(ImageType type) {
        this.imageType = type;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public Integer getImageSize() {
        return imageSize;
    }

    public void setImageSize(Integer imageSize) {
        this.imageSize = imageSize;
    }

    public String getImageHash() {
        return imageHash;
    }

    public void setImageHash(String imageHash) {
        this.imageHash = imageHash;
    }

    public Long getDeduplicatedImageId() {
        return deduplicatedImageId;
    }

    public void setDeduplicatedImageId(Long imageDataImageId) {
        this.deduplicatedImageId = imageDataImageId;
    }
}
