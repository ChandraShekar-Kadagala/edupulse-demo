package com.edupulse.dto;

import com.edupulse.model.AuthProvider;
import com.edupulse.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String username;
    private String name;
    private String email;
    private Role role;
    private AuthProvider authProvider;
}
