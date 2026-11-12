package com.ai.customer.support.dto;

import com.ai.customer.support.enums.Role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserUpdateRequest(@NotBlank(message = "First name is required") String firstName,
		@NotBlank(message = "Last name is required") String lastName, @NotNull(message = "Role is required") Role role,
		@NotNull(message = "Active status is required") Boolean active) {
}
