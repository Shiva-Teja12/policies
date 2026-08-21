package com.practice.springbootdemo.advance_performance_module.security;

import com.practice.springbootdemo.advance_performance_module.entities.Role;
import com.practice.springbootdemo.advance_performance_module.entities.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String employeeCode;
    private final String name;
    private final String email;
    private final String password;
    private final Role role;
    private final Long departmentId;
    private final boolean active;
    private final Collection<? extends GrantedAuthority> authorities;

    public static CustomUserDetails build(User user) {
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );

        return CustomUserDetails.builder()
                .id(user.getId())
                .employeeCode(user.getEmployeeCode())
                .name(user.getName())
                .email(user.getEmail())
                .password(user.getPasswordHash())
                .role(user.getRole())
                .departmentId(user.getDepartmentId())
                .active(user.isActive())
                .authorities(authorities)
                .build();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override
    public String getPassword() { return password; }
    @Override
    public String getUsername() { return email; }
    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return active; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return active; }
}