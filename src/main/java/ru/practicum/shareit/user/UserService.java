package ru.practicum.shareit.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getAllUsers() {
        return userRepository.getAllUsers();
    }

    public User createUser(User user) {
        return userRepository.create(user);
    }

    public User updateUser(Long userId, UserDto userDto) {
        User existingUser = userRepository.findUser(userId);

        if (userDto.getName() != null) {
            existingUser.setName(userDto.getName());
        }
        if (userDto.getEmail() != null) {
            existingUser.setEmail(userDto.getEmail());
        }

        return userRepository.update(existingUser);
    }

    public User findUserById(Long userId) {
        return userRepository.findUser(userId);
    }

    public void deleteUserById(Long userId) {
        userRepository.deleteUserById(userId);
    }


}
