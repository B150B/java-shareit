package ru.practicum.shareit.user.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.user.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@Slf4j
public class InMemoryUserRepository implements UserRepository {

    private final Map<Long, User> userMap = new HashMap<>();
    private long nextId = 1L;

    private Long getNextId() {
        return nextId++;
    }


    @Override
    public User create(User user) {
        checkEmailUnique(user);
        user.setId(getNextId());
        userMap.put(user.getId(), user);
        log.info("Добавлен пользователь с id {}", user.getId());
        return user;
    }

    @Override
    public User update(User user) {
        Long userId = user.getId();
        if (userId == null || !userMap.containsKey(userId)) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }
        checkEmailUnique(user);

        User updatedUser = userMap.get(userId);
        updatedUser.setName(user.getName());
        updatedUser.setEmail(user.getEmail());

        log.info("Обновлен пользователь с id {}", userId);
        return updatedUser;
    }

    @Override
    public List<User> getAllUsers() {
        log.info("Запрошен список всех пользователей");
        return userMap.values().stream().toList();
    }

    @Override
    public User findUser(Long userId) {
        if (!userMap.containsKey(userId)) {
            throw new NotFoundException("Пользователь с id " + userId + " не обнаружен");
        }
        log.info("Запрошен пользователь с id {}", userId);
        return userMap.get(userId);
    }

    @Override
    public void deleteUserById(Long userId) {
        if (!userMap.containsKey(userId)) {
            throw new NotFoundException("Пользователь с id " + userId + " не обнаружен");
        }
        userMap.remove(userId);
        log.info("Удален пользователь с id {}", userId);
    }


    private void checkEmailUnique(User user) {
        boolean emailExists = userMap.values().stream()
                .filter(user1 -> !user1.getId().equals(user.getId()) && user1.getEmail() != null)
                .anyMatch(user1 -> user1.getEmail().equals(user.getEmail()));
        if (emailExists) {
            throw new ValidationException("Этот email уже зарегистрирован на другого пользователя");
        }
    }
}
