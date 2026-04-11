package com.photovault.service;

import com.photovault.entity.Photographer;
import com.photovault.repository.PhotographerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final PhotographerRepository photographerRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Photographer photographer = photographerRepository.findActiveByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return User.builder()
            .username(photographer.getEmail())
            .password(photographer.getPasswordHash())
            .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_PHOTOGRAPHER")))
            .accountExpired(false)
            .accountLocked(false)
            .credentialsExpired(false)
            .disabled(photographer.getDeletedAt() != null)
            .build();
    }
}
