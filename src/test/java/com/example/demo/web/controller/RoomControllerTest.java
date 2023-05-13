package com.example.demo.web.controller;

import com.example.demo.persistence.dto.ConnectionRequestDto;
import com.example.demo.persistence.dto.RoomCreateDto;
import com.example.demo.persistence.dto.RoomInfoDto;
import com.example.demo.persistence.model.User;
import com.example.demo.persistence.model.enums.Role;
import com.example.demo.service.RoomService;
import com.example.demo.web.security.UserDetailsImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomControllerTest {

    @Mock
    private RoomService roomService;

    @InjectMocks
    private RoomController roomController;

    @Test
    void getOne_shouldReturnHttpStatusOKAndRoomId_whenRoomNameExists() {
        // Given
        String roomName = "Test Room";
        Long roomId = 1L;
        when(roomService.getRoomIdByName(roomName)).thenReturn(roomId);

        // When
        ResponseEntity<Long> response = roomController.getOne(roomName);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(roomId, response.getBody());
        verify(roomService).getRoomIdByName(roomName);
    }

    @Test
    void getAll_shouldReturnPageOfRoomInfoDto() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<RoomInfoDto> expectedPage = new PageImpl<>(List.of(new RoomInfoDto()));

        when(roomService.getAll(pageable)).thenReturn(expectedPage);

        // When
        Page<RoomInfoDto> actualPage = roomController.getAll(pageable);

        // Then
        assertEquals(expectedPage, actualPage);
        verify(roomService).getAll(pageable);
    }

    @Test
    void getAllUserRooms_shouldReturnPageOfRoomInfoDto() {
        // Given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<RoomInfoDto> expectedPage = new PageImpl<>(List.of(new RoomInfoDto()));

        when(roomService.getAllRoomsByUserId(userId, pageable)).thenReturn(expectedPage);

        // When
        Page<RoomInfoDto> actualPage = roomController.getAllUserRooms(userId, pageable);

        // Then
        assertEquals(expectedPage, actualPage);
        verify(roomService).getAllRoomsByUserId(userId, pageable);
    }

    @Test
    void create_shouldReturnHttpStatusOK_whenRoomDtoIsValid() {
        // Given
        RoomCreateDto roomDto = new RoomCreateDto();
        User user = User.builder()
                .id(1L)
                .email("email")
                .password("password")
                .role(Role.USER)
                .build();
        UserDetailsImpl currentUser = new UserDetailsImpl(user);
        doNothing().when(roomService).create(roomDto, currentUser.getId());

        // When
        ResponseEntity<HttpStatus> response = roomController.create(roomDto, currentUser);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(roomService).create(roomDto, currentUser.getId());
    }

    @Test
    void create_shouldReturnHttpStatusUnauthorized_whenCurrentUserIsNull() {
        // Given
        RoomCreateDto roomDto = new RoomCreateDto();
        UserDetailsImpl currentUser = null;

        // When
        ResponseEntity<HttpStatus> response = roomController.create(roomDto, currentUser);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void connect_shouldReturnHttpStatusOK_whenConnectionRequestDtoIsValid() {
        // Given
        Long roomId = 1L;
        ConnectionRequestDto connectionRequestDto = new ConnectionRequestDto();
        doNothing().when(roomService).connect(roomId, connectionRequestDto);

        // When
        ResponseEntity<HttpStatus> response = roomController.connect(roomId, connectionRequestDto);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(roomService).connect(roomId, connectionRequestDto);
    }
}