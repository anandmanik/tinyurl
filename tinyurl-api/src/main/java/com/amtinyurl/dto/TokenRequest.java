package com.amtinyurl.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TokenRequest {

    @NotBlank(message = "userId cannot be blank")
    @Size(min = 6, max = 6, message = "userId must be exactly 6 characters")
    @Pattern(regexp = "^[a-zA-Z0-9]{6}$", message = "userId must contain only alphanumeric characters")
    private String userId;
}