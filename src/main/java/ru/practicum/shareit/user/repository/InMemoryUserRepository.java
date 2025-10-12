package ru.practicum.shareit.user.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.user.User;

import java.util.*;

@Repository
@Slf4j
public class InMemoryUserRepository implements UserRepository {

    private final Map<Long, User> userMap = new HashMap<>();
    private final Set<String> registeredEmails = new HashSet<>();
    private long nextId = 1L;

    private Long getNextId() {
        return nextId++;
    }


    @Override
    public User create(User user) {
        checkEmailUnique(user);
        user.setId(getNextId());
        userMap.put(user.getId(), user);
        registeredEmails.add(user.getEmail());
        log.info("Добавлен пользователь с id {}", user.getId());
        return user;
    }

    @Override
    public User update(User user) {
        Long userId = user.getId();
        if (userId == null || !userMap.containsKey(userId)) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }

        User updatedUser = userMap.get(userId);

        if (user.getEmail() != null && !user.getEmail().equals(updatedUser.getEmail())) {
            checkEmailUnique(user);
            registeredEmails.remove(updatedUser.getEmail());
            registeredEmails.add(user.getEmail());
            updatedUser.setEmail(user.getEmail());
        }

        if (user.getName() != null) {
            updatedUser.setName(user.getName());
        }
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
        registeredEmails.remove(findUser(userId).getEmail());
        userMap.remove(userId);
        log.info("Удален пользователь с id {}", userId);
    }


    private void checkEmailUnique(User user) {
        if (registeredEmails.contains(user.getEmail())) {
            throw new ValidationException("Этот email уже зарегистрирован на другого пользователя");
        }
    }


}
