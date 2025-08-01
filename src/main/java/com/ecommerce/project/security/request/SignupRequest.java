package com.ecommerce.project.security.request;

import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class SignupRequest {
    @NotBlank
    @Size(min = 3, max = 20)
    private String username;
    @NotBlank
    @Size(min = 3, max = 40)
    private String password;
    @NotBlank
    @Size(min = 3, max = 50)
    private String email;
    private Set<String> role;
}
