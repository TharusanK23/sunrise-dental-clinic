package com.sunrise.dentalclinic.service.impl;

import com.sunrise.dentalclinic.dto.request.CreateUserRequest;
import com.sunrise.dentalclinic.dto.request.UpdateUserRequest;
import com.sunrise.dentalclinic.dto.response.UserResponse;
import com.sunrise.dentalclinic.entity.User;
import com.sunrise.dentalclinic.exception.DuplicateResourceException;
import com.sunrise.dentalclinic.exception.ResourceNotFoundException;
import com.sunrise.dentalclinic.repository.UserRepository;
import com.sunrise.dentalclinic.service.UserService;
import com.sunrise.dentalclinic.util.DtoMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Administers staff accounts. Only an ADMIN may create, edit or deactivate accounts (enforced in SecurityConfig / controller). */
@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(DtoMapper::toResponse).toList();
    }

    @Override
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("A staff account with username '" + request.username() + "' already exists.");
        }
        User user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .email(request.email())
                .role(request.role())
                .enabled(true)
                .build();
        return DtoMapper.toResponse(userRepository.save(user));
    }

    @Override
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff account not found with id: " + id));
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        return DtoMapper.toResponse(userRepository.save(user));
    }

    @Override
    public void deactivate(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff account not found with id: " + id));
        user.setEnabled(false);
        userRepository.save(user);
    }
}
