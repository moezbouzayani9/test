package com.valuephone.image.dto;

import javax.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * <p>
 * User: ahack Date: 22.09.17 Time: 14:54
 */
public class ImageDto  {

    @NotNull
    private String uuid;

    @NotNull
    private String name;

    public ImageDto() {}

    public ImageDto(String uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public ImageDto(UUID uuid, String name) {
        this.uuid = uuid.toString();
        this.name = name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
