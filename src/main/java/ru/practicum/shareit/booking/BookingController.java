package ru.practicum.shareit.booking;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;

import java.util.List;

import static ru.practicum.shareit.utility.HeaderConstants.SHARER_USER_HEADER;


@RestController
@RequestMapping(path = "/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public BookingDto createBooking(
            @RequestHeader(SHARER_USER_HEADER) Long userId,
            @RequestBody @Valid BookingCreateDto bookingCreateDto
    ) {
        return bookingService.createBooking(userId, bookingCreateDto);
    }


    @PatchMapping("/{bookingId}")
    public BookingDto approveBooking(
            @RequestHeader(SHARER_USER_HEADER) Long ownerId,
            @PathVariable Long bookingId,
            @RequestParam boolean approved
    ) {
        return bookingService.changeApproval(ownerId, bookingId, approved);
    }

    @GetMapping("/{bookingId}")
    public BookingDto getBooking(
            @RequestHeader(SHARER_USER_HEADER) Long userId,
            @PathVariable Long bookingId
    ) {
        return bookingService.getBookingById(userId, bookingId);
    }

    @GetMapping
    public List<BookingDto> getBookingByUser(
            @RequestHeader(SHARER_USER_HEADER) Long userId,
            @RequestParam(defaultValue = "ALL") String state
    ) {
        return bookingService.getBookingForUser(userId, state);
    }

    @GetMapping("/owner")
    public List<BookingDto> getBookingsByOwner(
            @RequestHeader(SHARER_USER_HEADER) Long ownerId,
            @RequestParam(defaultValue = "ALL") String state
    ) {
        return bookingService.getBookingsForOwner(ownerId, state);
    }


}
