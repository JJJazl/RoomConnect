package com.example.demo.service.impl;

import com.example.demo.exception.UserAlreadyExists;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.persistence.dto.UserCreateDto;
import com.example.demo.persistence.dto.UserProfileReadDto;
import com.example.demo.persistence.dto.UserProfileUpdateDto;
import com.example.demo.persistence.model.User;
import com.example.demo.persistence.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock
    private UserRepository userRepo;
    @Mock
    private PasswordEncoder encoder;
    @Mock
    private ModelMapper modelMapper;
    @InjectMocks
    private UserServiceImpl userService;

    @Test
    public void testAddUser() {
        UserCreateDto userCreateDto = new UserCreateDto();
        userCreateDto.setId(1L);
        userCreateDto.setEmail("test@example.com");
        userCreateDto.setUsername("testuser");
        userCreateDto.setPassword("password");

        User expectedUser = new User();
        expectedUser.setId(1L);
        expectedUser.setEmail("test@example.com");
        expectedUser.setUsername("testuser");
        expectedUser.setPassword(encoder.encode("password"));

        when(userRepo.existsByEmail("test@example.com")).thenReturn(false);
        when(encoder.encode(userCreateDto.getPassword())).thenReturn("EncodedPassword");
        when(modelMapper.map(userCreateDto, User.class)).thenReturn(expectedUser);
        when(userRepo.save(any(User.class))).thenReturn(expectedUser);
        when(modelMapper.map(expectedUser, UserCreateDto.class)).thenReturn(userCreateDto);

        UserCreateDto result = userService.addUser(userCreateDto);

        assertEquals(userCreateDto.getEmail(), result.getEmail());
        assertEquals(userCreateDto.getUsername(), result.getUsername());
        assertNotNull(result.getId());
    }

    @Test
    public void testAddUserEmailAlreadyExists() {
        UserCreateDto userCreateDto = new UserCreateDto();
        userCreateDto.setEmail("test@example.com");
        userCreateDto.setUsername("testuser");
        userCreateDto.setPassword("password");

        when(userRepo.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(UserAlreadyExists.class, () -> userService.addUser(userCreateDto));
    }

    @Test
    public void testGetById() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setUsername("testuser");

        UserProfileReadDto result = new UserProfileReadDto();
        result.setId(1L);
        result.setEmail("test@example.com");
        result.setUsername("testuser");

        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(modelMapper.map(user, UserProfileReadDto.class)).thenReturn(result);

        userService.getById(1L);

        assertEquals(user.getId(), result.getId());
        assertEquals(user.getEmail(), result.getEmail());
        assertEquals(user.getUsername(), result.getUsername());
    }

    @Test
    public void testGetByIdInvalid() {
        when(userRepo.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getById(1L));
    }

    @Test
    public void testUpdateById() {
        UserProfileUpdateDto userDto = new UserProfileUpdateDto();
        userDto.setUsername("newusername");
        userDto.setEmail("newemail@example.com");
        userDto.setPassword("newpassword");
        userDto.setConfirmPassword("newpassword");

        User user = new User();
        user.setId(1L);
        user.setUsername("oldusername");
        user.setEmail("oldemail@example.com");
        user.setPassword(encoder.encode("oldpassword"));

        when(userRepo.findById(1L)).thenReturn(Optional.of(user));

        userService.updateById(1L, userDto);

        assertEquals(userDto.getUsername(), user.getUsername());
        assertEquals(userDto.getEmail(), user.getEmail());
        assertEquals(userDto.getImageUrl(), user.getImageUrl());
        assertNotEquals(userDto.getPassword(), user.getPassword());
    }

    @Test
    public void testDeleteById() {
        User user = new User();
        user.setId(1L);

        when(userRepo.findById(1L)).thenReturn(Optional.of(user));

        assertTrue(userService.deleteById(1L));
    }

    @Test
    public void testDeleteByIdInvalid() {
        when(userRepo.findById(1L)).thenReturn(Optional.empty());

        assertFalse(userService.deleteById(1L));
    }
}