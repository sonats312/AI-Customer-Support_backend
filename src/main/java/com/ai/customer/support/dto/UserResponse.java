package com.ai.customer.support.dto;

import com.ai.customer.support.enums.Role;

public record UserResponse(Long id, String email, String firstName, String lastName, Role role, Boolean active) {
}
