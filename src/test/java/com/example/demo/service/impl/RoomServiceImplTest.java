package com.example.demo.service.impl;

import com.example.demo.exception.RoomConnectionException;
import com.example.demo.exception.RoomNotFoundException;
import com.example.demo.persistence.dto.ConnectedUserDto;
import com.example.demo.persistence.dto.ConnectionRequestDto;
import com.example.demo.persistence.dto.RoomCreateDto;
import com.example.demo.persistence.dto.RoomInfoDto;
import com.example.demo.persistence.model.Room;
import com.example.demo.persistence.model.User;
import com.example.demo.persistence.repository.RoomRepository;
import com.example.demo.persistence.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceImplTest {
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ModelMapper modelMapper;
    @InjectMocks
    private RoomServiceImpl roomService;

    @Test
    public void createShouldSaveRoom() {
        // Given
        Long userId = 1L;
        RoomCreateDto roomDto = new RoomCreateDto();
        User user = new User();
        when(userRepository.getReferenceById(userId)).thenReturn(user);
        when(modelMapper.map(roomDto, Room.class)).thenReturn(new Room());

        // When
        roomService.create(roomDto, userId);

        // Then
        verify(roomRepository).save(any(Room.class));
    }

    @Test
    public void getRoomIdByNameShouldReturnId() {
        // Given
        String name = "test";
        Room room = new Room();
        room.setId(1L);
        when(roomRepository.findByName(name)).thenReturn(Optional.of(room));

        // When
        Long result = roomService.getRoomIdByName(name);

        // Then
        assertEquals(1L, result.longValue());
    }

    @Test
    public void getRoomIdByNameShouldThrowException() {
        // Given
        String name = "test";
        when(roomRepository.findByName(name)).thenReturn(Optional.empty());

        // When and Then
        assertThrows(RoomNotFoundException.class, () -> {
            roomService.getRoomIdByName(name);
        });
    }

    @Test
    public void getAllShouldReturnPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Room> page = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(roomRepository.findAll(pageable)).thenReturn(page);

        // When
        Page<RoomInfoDto> result = roomService.getAll(pageable);

        // Then
        assertTrue(result.getContent().isEmpty());
        assertEquals(page.getTotalElements(), result.getTotalElements());
    }

    @Test
    public void getAllRoomsByUserIdShouldReturnPage() {
        // Given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Room> page = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(roomRepository.findAllByUserId(userId, pageable)).thenReturn(page);

        // When
        Page<RoomInfoDto> result = roomService.getAllRoomsByUserId(userId, pageable);

        // Then
        assertTrue(result.getContent().isEmpty());
        assertEquals(page.getTotalElements(), result.getTotalElements());
    }

    @Test
    void connectShouldAddConnectedUser() {
        // Given
        Long roomId = 1L;
        ConnectionRequestDto connectionRequestDto = new ConnectionRequestDto();
        connectionRequestDto.setUsername("test");
        connectionRequestDto.setPassword("password");
        Room room = Room.builder()
                .id(roomId)
                .name("Test Room")
                .isPrivate(false)
                .password("password")
                .numberOfUsers(2)
                .build();
        User user = User.builder()
                .id(1L)
                .username("test")
                .imageUrl("test_image_url")
                .build();

        List<ConnectedUserDto> connectedUsers = new ArrayList<>();
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(userRepository.findByUsername(connectionRequestDto.getUsername()))
                .thenReturn(Optional.of(user));

        // When
        roomService.connect(roomId, connectionRequestDto);

        // Then
        verify(roomRepository).findById(roomId);
        verify(userRepository).findByUsername(connectionRequestDto.getUsername());

        List<ConnectedUserDto> actualConnectedUsers = roomService.connections.get(roomId);
        assertEquals(1, actualConnectedUsers.size());
        ConnectedUserDto actualConnectedUser = actualConnectedUsers.get(0);
        assertEquals(connectionRequestDto.getUsername(), actualConnectedUser.getUsername());
        assertEquals("test_image_url", actualConnectedUser.getImageUrl());
    }

    @Test
    void connectShouldThrowRoomNotFoundException() {
        // Given
        Long roomId = 1L;
        ConnectionRequestDto connectionRequestDto = new ConnectionRequestDto();
        connectionRequestDto.setUsername("test");
        connectionRequestDto.setPassword("password");
        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

        // When and Then
        assertThrows(RoomNotFoundException.class, () -> roomService.connect(roomId, connectionRequestDto));
    }

    @Test
    void connectShouldThrowRoomConnectionExceptionIfRoomIsFull() {
        // Given
        Long roomId = 1L;
        ConnectionRequestDto connectionRequestDto1 = new ConnectionRequestDto();
        connectionRequestDto1.setUsername("user1");
        connectionRequestDto1.setPassword("password");
        ConnectionRequestDto connectionRequestDto2 = new ConnectionRequestDto();
        connectionRequestDto2.setUsername("user2");
        connectionRequestDto2.setPassword("password");
        Room room = new Room();
        room.setId(roomId);
        room.setName("Test Room");
        room.setPrivate(false);
        room.setPassword("password");
        room.setNumberOfUsers(1);
        List<ConnectedUserDto> connectedUsers = new ArrayList<>();
        connectedUsers.add(new ConnectedUserDto("user1", null));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(userRepository.findByUsername(connectionRequestDto1.getUsername())).thenReturn(Optional.of(new User()));

        // When and Then
        roomService.connect(roomId, connectionRequestDto1);
        assertThrows(RoomConnectionException.class, () -> roomService.connect(roomId, connectionRequestDto2));
    }

    @Test
    void connectShouldThrowRoomConnectionExceptionIfWrongPassword() {
        // Given
        Long roomId = 1L;
        ConnectionRequestDto connectionRequestDto = new ConnectionRequestDto();
        connectionRequestDto.setUsername("test");
        connectionRequestDto.setPassword("wrongpassword");
        Room room = new Room();
        room.setId(roomId);
        room.setName("Test Room");
        room.setPrivate(true);
        room.setPassword("password");
        room.setNumberOfUsers(2);
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));

        // When and Then
        assertThrows(RoomConnectionException.class, () -> roomService.connect(roomId, connectionRequestDto));
    }
}