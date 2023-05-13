package com.example.demo.web.controller;

import com.example.demo.persistence.dto.UserCreateDto;
import com.example.demo.persistence.dto.UserProfileReadDto;
import com.example.demo.persistence.dto.UserProfileUpdateDto;
import com.example.demo.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserControllerTest {
    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @Test
    void registerShouldReturnUserCreateDto() {
        // Given
        UserCreateDto userCreateDto = new UserCreateDto();
        userCreateDto.setUsername("test");
        when(userService.addUser(userCreateDto)).thenReturn(userCreateDto);

        // When
        ResponseEntity<UserCreateDto> responseEntity = userController.register(userCreateDto);

        // Then
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(userCreateDto, responseEntity.getBody());
    }

    @Test
    void getUserByIdShouldReturnUserProfileReadDto() {
        // Given
        Long userId = 1L;
        UserProfileReadDto userProfileReadDto = new UserProfileReadDto();
        when(userService.getById(userId)).thenReturn(userProfileReadDto);

        // When
        ResponseEntity<UserProfileReadDto> responseEntity = userController.getUserById(userId);

        // Then
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(userProfileReadDto, responseEntity.getBody());
    }

    @Test
    void getUserByIdShouldThrowAccessDeniedException() {
        // Given
        Long userId = 1L;
        when(userService.getById(userId)).thenThrow(new AccessDeniedException("Access denied"));

        // When and Then
        assertThrows(AccessDeniedException.class, () -> userController.getUserById(userId));
    }

    @Test
    void updateByIdShouldReturnHttpStatusOk() {
        // Given
        Long userId = 1L;
        UserProfileUpdateDto userProfileUpdateDto = new UserProfileUpdateDto();

        // When
        ResponseEntity<HttpStatus> responseEntity = userController.updateById(userId, userProfileUpdateDto);

        // Then
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        verify(userService).updateById(userId, userProfileUpdateDto);
    }

    @Test
    void deleteUserByIdShouldReturnHttpStatusNoContent() {
        // Given
        Long userId = 1L;
        when(userService.deleteById(userId)).thenReturn(true);

        // When
        ResponseEntity<?> responseEntity = userController.deleteUserById(userId);

        // Then
        assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
        verify(userService).deleteById(userId);
    }

    @Test
    void deleteUserByIdShouldReturnHttpStatusNotFound() {
        // Given
        Long userId = 1L;
        when(userService.deleteById(userId)).thenReturn(false);

        // When
        ResponseEntity<?> responseEntity = userController.deleteUserById(userId);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        verify(userService).deleteById(userId);
    }
}