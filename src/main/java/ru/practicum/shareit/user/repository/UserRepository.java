package ru.practicum.shareit.user.repository;

import ru.practicum.shareit.user.User;

import java.util.List;

public interface UserRepository {
    User create(User user);

    User update(User user);

    List<User> getAllUsers();

    User findUser(Long userId);

    void deleteUserById(Long userId);

}
