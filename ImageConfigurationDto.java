package com.valuephone.image.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ImageConfigurationDto {
    private Integer minWidth;

    private Integer maxWidth;

    private Integer minHeight;

    private Integer maxHeight;

    private Integer maxFileSize;
}
