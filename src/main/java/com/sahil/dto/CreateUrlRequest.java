package com.sahil.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUrlRequest {
    @NotBlank(message = "Original URL is required")
    @Pattern(
        regexp = "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$",
        message = "Invalid URL format"
    )
    private String originalUrl;

    @Size(max = 10, message = "Custom short code must not exceed 10 characters")
    private String customShortCode;

    private LocalDateTime expiresAt;
}
