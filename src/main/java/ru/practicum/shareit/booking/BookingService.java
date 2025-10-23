package ru.practicum.shareit.booking;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingMapper;
import ru.practicum.shareit.exception.AccessError;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ItemRepository itemRepository;
    private final UserService userService;

    public BookingService(BookingRepository bookingRepository,
                          ItemRepository itemRepository,
                          UserService userService) {
        this.bookingRepository = bookingRepository;
        this.itemRepository = itemRepository;
        this.userService = userService;
    }

    public BookingDto createBooking(Long userId, BookingCreateDto dto) {
        User booker = userService.findUserEntityById(userId);
        Item item = itemRepository.findById(dto.getItemId())
                .orElseThrow(() -> new NotFoundException("Вещь с id " + dto.getItemId() + " не найдена"));

        if (Objects.equals(item.getOwner().getId(), userId)) {
            throw new NotFoundException("Нельзя бронировать собственную вешь");
        }

        if (!Boolean.TRUE.equals(item.getAvailable())) {
            throw new ValidationException("Вещь недоступна для бронирования");
        }

        if (dto.getStart() == null || dto.getEnd() == null || !dto.getStart().isBefore(dto.getEnd())) {
            throw new ValidationException("Некккоректные даты бронирования");
        }

        Booking booking = BookingMapper.toBookingFromCreateDto(dto);
        booking.setItem(item);
        booking.setBooker(booker);
        booking.setStatus(BookingStatus.WAITING);

        return BookingMapper.toBookingDto(bookingRepository.save(booking));
    }

    public BookingDto changeApproval(Long ownerId, Long bookingId, boolean approved) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Бронирование не найдено"));

        if (!Objects.equals(booking.getItem().getOwner().getId(), ownerId)) {
            throw new AccessError("Подтвердить бронирование может только владелец вещи");
        }

        if (booking.getStatus() == BookingStatus.APPROVED && approved) {
            throw new ValidationException("Бронирование уже подтверждено");
        }

        booking.setStatus(approved ? BookingStatus.APPROVED : BookingStatus.REJECTED);
        return BookingMapper.toBookingDto(bookingRepository.save(booking));
    }


    @Transactional(readOnly = true)
    public BookingDto getBookingById(Long userId, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Бронирование не найдено"));

        Long ownerId = booking.getItem().getOwner().getId();
        Long bookerId = booking.getBooker().getId();

        if (!Objects.equals(userId, ownerId) && !Objects.equals(userId, bookerId)) {
            throw new AccessError("Доступ запрещен");
        }

        return BookingMapper.toBookingDto(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingDto> getBookingForUser(Long userId, String state) {
        User user = userService.findUserEntityById(userId);
        List<Booking> bookings = bookingRepository.findByBookerOrderByStartDesc(user);
        return filterBookingsByState(bookings, state).stream()
                .map(BookingMapper::toBookingDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookingDto> getBookingsForOwner(Long ownerId, String state) {
        List<Item> items = itemRepository.findByOwnerId(ownerId);
        if (items.isEmpty()) throw new NotFoundException("Список вещей пуст");

        List<Booking> bookings = bookingRepository.findByItemInOrderByStartDesc(items);
        return filterBookingsByState(bookings, state).stream()
                .map(BookingMapper::toBookingDto)
                .collect(Collectors.toList());
    }


    private List<Booking> filterBookingsByState(List<Booking> bookings, String state) {
        LocalDateTime now = LocalDateTime.now();
        if (state == null || state.isBlank() || state.equalsIgnoreCase("ALL")) return bookings;


        switch (state.toUpperCase(Locale.ROOT)) {
            case "CURRENT":
                return bookings.stream()
                        .filter(b -> b.getStart().isBefore(now) && b.getEnd().isAfter(now))
                        .collect(Collectors.toList());
            case "PAST":
                return bookings.stream()
                        .filter(b -> b.getEnd().isBefore(now))
                        .collect(Collectors.toList());
            case "FUTURE":
                return bookings.stream()
                        .filter(b -> b.getStart().isAfter(now))
                        .collect(Collectors.toList());
            case "WAITING":
                return bookings.stream()
                        .filter(b -> b.getStatus() == BookingStatus.WAITING)
                        .collect(Collectors.toList());
            case "REJECTED":
                return bookings.stream()
                        .filter(b -> b.getStatus() == BookingStatus.REJECTED)
                        .collect(Collectors.toList());
            default:
                throw new ValidationException("Unknown state: " + state);
        }


    }


}
