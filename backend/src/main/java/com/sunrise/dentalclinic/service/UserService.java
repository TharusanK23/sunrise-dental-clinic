package com.sunrise.dentalclinic.service;

import com.sunrise.dentalclinic.dto.request.CreateUserRequest;
import com.sunrise.dentalclinic.dto.request.UpdateUserRequest;
import com.sunrise.dentalclinic.dto.response.UserResponse;

import java.util.List;

public interface UserService {
    List<UserResponse> findAll();
    UserResponse create(CreateUserRequest request);
    UserResponse update(Long id, UpdateUserRequest request);
    void deactivate(Long id);
}
