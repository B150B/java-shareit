package ru.practicum.shareit.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.dto.UserMapper;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserDto> getAllUsers() {
        return userRepository.getAllUsers().stream()
                .map(UserMapper::toUserDto)
                .collect(Collectors.toList());
    }

    public UserDto createUser(UserDto userDto) {
        return UserMapper.toUserDto(userRepository.create(UserMapper.toUser(userDto)));
    }

    public UserDto updateUser(Long userId, UserDto userDto) {
        User updatingUser = UserMapper.toUser(userDto);
        updatingUser.setId(userId);

        return UserMapper.toUserDto(userRepository.update(updatingUser));
    }

    public UserDto findUserById(Long userId) {
        return UserMapper.toUserDto(userRepository.findUser(userId));
    }

    public void deleteUserById(Long userId) {
        userRepository.deleteUserById(userId);
    }


}
