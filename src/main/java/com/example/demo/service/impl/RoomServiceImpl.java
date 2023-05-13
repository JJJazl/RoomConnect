package com.example.demo.service.impl;

import com.example.demo.exception.RoomConnectionException;
import com.example.demo.exception.RoomNotFoundException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.persistence.dto.*;
import com.example.demo.persistence.model.Room;
import com.example.demo.persistence.model.User;
import com.example.demo.persistence.repository.RoomRepository;
import com.example.demo.persistence.repository.UserRepository;
import com.example.demo.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    public final Map<Long, List<ConnectedUserDto>> connections = new ConcurrentHashMap<>();
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public void create(RoomCreateDto roomDto, Long userId) {
        User user = userRepository.getReferenceById(userId);
        Room room = modelMapper.map(roomDto, Room.class);
        room.setUser(user);
        roomRepository.save(room);
    }

    @Override
    public Long getRoomIdByName(String name) {
        return roomRepository.findByName(name)
                .orElseThrow(() -> new RoomNotFoundException("Can not find room by name: " + name)).getId();
    }

    @Override
    public Page<RoomInfoDto> getAll(Pageable pageable) {
        return getRoomInfoDtos(null, pageable);
    }

    @Override
    public Page<RoomInfoDto> getAllRoomsByUserId(Long userId, Pageable pageable) {
        return getRoomInfoDtos(userId, pageable);
    }

    private PageImpl<RoomInfoDto> getRoomInfoDtos(@Nullable Long userId, Pageable pageable) {
        PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
        var page = getPageByUserId(userId, pageable);
        var roomInfoDtos = page.stream()
                .map(this::mapToRoomInfoDto)
                .toList();
        return new PageImpl<>(roomInfoDtos, pageRequest, page.getTotalElements());
    }

    private Page<Room> getPageByUserId(Long userId, Pageable pageable) {
        return userId == null
                ? roomRepository.findAll(pageable)
                : roomRepository.findAllByUserId(userId, pageable);
    }

    @Override
    public void connect(Long id, ConnectionRequestDto requestDto) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RoomNotFoundException("Can not find room by id: " + id));
        validateRoomPassword(room, requestDto.getPassword());

        validateRoomConnection(requestDto, room);
    }

    private void validateRoomConnection(ConnectionRequestDto connectionRequestDto, Room room) {
        List<ConnectedUserDto> connectedUsers = connections.computeIfAbsent(
                room.getId(), key -> new ArrayList<>(room.getNumberOfUsers()));
        if (connectedUsers.size() < room.getNumberOfUsers()) {
            ConnectedUserDto connectedUser = createConnectedUser(connectionRequestDto.getUsername());
            connectedUsers.add(connectedUser);
        } else {
            throw new RoomConnectionException("Connection failed! Room is full!");
        }
    }

    @Override
    public void disconnect(DisconnectionRequestDto requestDto) {
        String roomName = requestDto.getRoomName();
        Room room = roomRepository.findByName(roomName)
                .orElseThrow(() -> new RoomNotFoundException("Can not find room by name: " + roomName));
        Long roomId = room.getId();

        List<ConnectedUserDto> connectedUsers = validateConnectionCapacity(roomId);
        disconnectUser(requestDto, connectedUsers);

        if (connectedUsers.isEmpty()) {
            connections.remove(roomId);
        }
    }

    private static void disconnectUser(DisconnectionRequestDto requestDto, List<ConnectedUserDto> connectedUsers) {
        String username = requestDto.getUsername();
        String roomName = requestDto.getRoomName();
        var connectedUser = connectedUsers.stream()
                .filter(user -> username.equals(user.getUsername()))
                .findAny()
                .orElseThrow(() -> new UserNotFoundException(
                        String.format("Can not find user in room %s by username: %s", roomName, username)));
        connectedUsers.remove(connectedUser);
    }

    private List<ConnectedUserDto> validateConnectionCapacity(Long roomId) {
        var connectedUsers = connections.get(roomId);
        if (connectedUsers == null) {
            throw new RoomConnectionException("Disconnection failed! Room is empty!");
        }
        return connectedUsers;
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RoomNotFoundException("Can not find room by id: " + id));
        roomRepository.delete(room);
    }

    private ConnectedUserDto createConnectedUser(String username) {
        String imageUrl = loadImageUrlByUsername(username);
        return ConnectedUserDto.builder().username(username).imageUrl(imageUrl).build();
    }

    private String loadImageUrlByUsername(String username) {
        return userRepository.findByUsername(username).map(User::getImageUrl).orElse(null);
    }

    private static void validateRoomPassword(Room room, String password) {
        if (room.isPrivate() && !room.getPassword().equals(password)) {
            throw new RoomConnectionException("Connection failed! Wrong password!");
        }
    }

    private RoomInfoDto mapToRoomInfoDto(Room room) {
        var roomInfoDto = modelMapper.map(room, RoomInfoDto.class);
        var connectedUsers = connections.get(room.getId());
        roomInfoDto.setConnectedUsers(connectedUsers);
        return roomInfoDto;
    }
}
