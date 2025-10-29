package ru.practicum.shareit.booking;

import lombok.RequiredArgsConstructor;
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
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ItemRepository itemRepository;
    private final UserService userService;


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

        List<Booking> existingsBookings = bookingRepository.findByItemIdAndStatus(item.getId(), BookingStatus.APPROVED);

        boolean overlaps = existingsBookings.stream().anyMatch(existing ->
                dto.getStart().isBefore(existing.getEnd()) &&
                        dto.getEnd().isAfter(existing.getStart())
        );

        if (overlaps) {
            throw new ValidationException("Вещь уже забронирована на указанный период");
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

        if (booking.getStatus() != BookingStatus.WAITING) {
            throw new ValidationException("Бронирование уже обработано и не может быть изменено");
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
        userService.findUserEntityById(userId);
        return getBookingsByState(userId, state);
    }

    @Transactional(readOnly = true)
    public List<BookingDto> getBookingsForOwner(Long ownerId, String state) {
        if (itemRepository.findByOwnerId(ownerId).isEmpty()) {
            throw new NotFoundException("Список вещей пуст");
        }
        return getBookingsByStateForOwner(ownerId, state);
    }


    public List<BookingDto> getBookingsByState(Long userId, String state) {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings;

        switch (state == null ? "ALL" : state.toUpperCase(Locale.ROOT)) {
            case "CURRENT":
                bookings = bookingRepository.findByBookerIdAndStartBeforeAndEndAfterOrderByStartDesc(userId, now, now);
                break;
            case "PAST":
                bookings = bookingRepository.findByBookerIdAndEndBeforeOrderByStartDesc(userId, now);
                break;
            case "FUTURE":
                bookings = bookingRepository.findByBookerIdAndStartAfterOrderByStartDesc(userId, now);
                break;
            case "WAITING":
                bookings = bookingRepository.findByBookerIdAndStatusOrderByStartDesc(userId, BookingStatus.WAITING);
                break;
            case "REJECTED":
                bookings = bookingRepository.findByBookerIdAndStatusOrderByStartDesc(userId, BookingStatus.REJECTED);
                break;
            case "ALL":
            default:
                bookings = bookingRepository.findByBookerIdOrderByStartDesc(userId);
                break;
        }

        return bookings.stream()
                .map(BookingMapper::toBookingDto)
                .collect(Collectors.toList());
    }

    public List<BookingDto> getBookingsByStateForOwner(Long ownerId, String state) {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings;

        switch (state == null ? "ALL" : state.toUpperCase(Locale.ROOT)) {
            case "CURRENT":
                bookings = bookingRepository.findByItemOwnerIdAndStartBeforeAndEndAfterOrderByStartDesc(ownerId, now, now);
                break;
            case "PAST":
                bookings = bookingRepository.findByItemOwnerIdAndEndBeforeOrderByStartDesc(ownerId, now);
                break;
            case "FUTURE":
                bookings = bookingRepository.findByItemOwnerIdAndStartAfterOrderByStartDesc(ownerId, now);
                break;
            case "WAITING":
                bookings = bookingRepository.findByItemOwnerIdAndStatusOrderByStartDesc(ownerId, BookingStatus.WAITING);
                break;
            case "REJECTED":
                bookings = bookingRepository.findByItemOwnerIdAndStatusOrderByStartDesc(ownerId, BookingStatus.REJECTED);
                break;
            case "ALL":
            default:
                bookings = bookingRepository.findByItemOwnerIdOrderByStartDesc(ownerId);
                break;
        }

        return bookings.stream()
                .map(BookingMapper::toBookingDto)
                .collect(Collectors.toList());
    }

    public boolean hasPastBooking(Long userId, Long itemId) {
        return bookingRepository.existsByBooker_IdAndItem_IdAndEndBefore(
                userId, itemId, LocalDateTime.now()
        );
    }

    public Map<Long, List<Booking>> getBookingsForItems(List<Item> items) {
        if (items.isEmpty()) return Map.of();

        List<Booking> allBookings = bookingRepository.findByItemInOrderByStartAsc(items);

        return allBookings.stream()
                .collect(Collectors.groupingBy(b -> b.getItem().getId()));
    }

    public BookingDto getLastAndNextBookingForItem(Item item) {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings = bookingRepository.findByItemIdOrderByStartAsc(item.getId());

        LocalDateTime lastBooking = bookings.stream()
                .filter(b -> b.getEnd().isBefore(now) && b.getStatus() == BookingStatus.APPROVED)
                .map(Booking::getEnd)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        LocalDateTime nextBooking = bookings.stream()
                .filter(b -> b.getStart().isAfter(now) && b.getStatus() == BookingStatus.APPROVED)
                .map(Booking::getStart)
                .min(LocalDateTime::compareTo)
                .orElse(null);

        BookingDto dto = new BookingDto();
        dto.setStart(lastBooking);
        dto.setEnd(nextBooking);
        return dto;
    }


}
