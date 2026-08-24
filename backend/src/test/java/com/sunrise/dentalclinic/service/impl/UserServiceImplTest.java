package com.sunrise.dentalclinic.service.impl;

import com.sunrise.dentalclinic.dto.request.CreateUserRequest;
import com.sunrise.dentalclinic.dto.request.UpdateUserRequest;
import com.sunrise.dentalclinic.dto.response.UserResponse;
import com.sunrise.dentalclinic.entity.Role;
import com.sunrise.dentalclinic.entity.User;
import com.sunrise.dentalclinic.exception.DuplicateResourceException;
import com.sunrise.dentalclinic.exception.ResourceNotFoundException;
import com.sunrise.dentalclinic.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Covers the admin "edit staff details" feature (name/email only) as well as account creation. */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("Updates only the full name and email of an existing staff account, leaving username/password/role untouched")
    void updatesStaffNameAndEmailOnly() {
        User existing = User.builder().id(1L).username("kirisha").password("hashed").fullName("Old Name")
                .email("old@sunrise.lk").role(Role.STAFF).enabled(true).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.update(1L, new UpdateUserRequest("New Name", "new@sunrise.lk"));

        assertThat(response.fullName()).isEqualTo("New Name");
        assertThat(response.email()).isEqualTo("new@sunrise.lk");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("kirisha");
        assertThat(saved.getPassword()).isEqualTo("hashed");
        assertThat(saved.getRole()).isEqualTo(Role.STAFF);
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when updating a staff account id that does not exist")
    void rejectsUpdateForUnknownUser() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update(99L, new UpdateUserRequest("X", "x@sunrise.lk")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Creates a new staff account with a BCrypt-encoded password when the username is not already taken")
    void createsNewStaffAccount() {
        when(userRepository.existsByUsername("newstaff")).thenReturn(false);
        when(passwordEncoder.encode("Passw0rd!")).thenReturn("$2b$10$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.create(new CreateUserRequest("newstaff", "Passw0rd!", "New Staff", "new@sunrise.lk", Role.STAFF));

        assertThat(response.username()).isEqualTo("newstaff");
        verify(passwordEncoder).encode("Passw0rd!");
    }

    @Test
    @DisplayName("Rejects creating a staff account with a username that already exists")
    void rejectsDuplicateUsername() {
        when(userRepository.existsByUsername("kirisha")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(new CreateUserRequest("kirisha", "Passw0rd!", "Dup", "dup@sunrise.lk", Role.STAFF)))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
    }
}
