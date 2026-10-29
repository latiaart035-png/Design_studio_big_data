package com.designstudio.application.service;

import com.designstudio.application.service.UserService;
import com.designstudio.domain.entity.User;
import com.designstudio.domain.repository.OrderItemRepository;
import com.designstudio.domain.repository.OrderRepository;
import com.designstudio.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void register_shouldEncodePasswordAndSaveUser() {
        // Arrange
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword("plainPassword");

        when(userRepository.findByEmail("test@example.com")).thenReturn(null);
        when(passwordEncoder.encode("plainPassword")).thenReturn("hashedPassword");

        // Act
        userService.register(user);

        // Assert
        verify(userRepository, times(1)).save(any(User.class));
        assertEquals("hashedPassword", user.getPassword());
        assertEquals(User.Role.USER, user.getRole());
        assertEquals(User.Status.NEW, user.getStatus());
    }

    @Test
    void register_shouldThrowException_whenEmailExists() {
        // Arrange
        User user = new User();
        user.setEmail("existing@example.com");
        when(userRepository.findByEmail("existing@example.com")).thenReturn(new User());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> userService.register(user));
        verify(userRepository, never()).save(any(User.class));
    }
}