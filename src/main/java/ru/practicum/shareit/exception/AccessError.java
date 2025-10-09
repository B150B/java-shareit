package ru.practicum.shareit.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND) //Не знаю почему так, но тесты просят чтобы возвращался 404 при такой ошибке
public class AccessError extends RuntimeException {
    public AccessError(String message) {
        super(message);
    }
}
