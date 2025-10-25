package ru.practicum.shareit.item;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingService;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.exception.AccessError;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.*;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ItemService {
    private final ItemRepository itemRepository;
    private final UserService userService;
    private final CommentService commentService;
    private final BookingService bookingService;

    @Autowired
    public ItemService(ItemRepository itemRepository, UserService userService, CommentService commentService, BookingService bookingService) {
        this.itemRepository = itemRepository;
        this.userService = userService;
        this.commentService = commentService;
        this.bookingService = bookingService;
    }

    public ItemDto createItem(Long userId, ItemDto itemDto) {
        User owner = userService.findUserEntityById(userId);

        Item item = ItemMapper.toItem(itemDto);
        item.setOwner(owner);

        Item savedItem = itemRepository.save(item);
        return ItemMapper.toItemDto(savedItem);
    }

    public List<ItemDto> getAllItemsByOwner(Long ownerId) {
        List<Item> items = itemRepository.findByOwnerId(ownerId);
        return items.stream()
                .map(ItemMapper::toItemDto)
                .collect(Collectors.toList());
    }

    public ItemDto updateItem(Long userId, Long itemId, ItemDto itemDto) {
        Item existingItem = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь с id " + itemId + " не найдена"));

        if (!userId.equals(existingItem.getOwner().getId())) {
            throw new AccessError("Обновлять вещь может только владелец");
        }

        ItemMapper.updateItemFromDto(itemDto, existingItem);

        Item updatedItem = itemRepository.save(existingItem);
        return ItemMapper.toItemDto(updatedItem);

    }

    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    public ItemDto findItemById(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь с id " + itemId + " не найдена"));
        return ItemMapper.toItemDto(item);
    }

    public void deleteItemById(Long itemId) {
        if (!itemRepository.existsById(itemId)) {
            throw new NotFoundException("Вещь с id " + itemId + " не найдена");
        }
        itemRepository.deleteById(itemId);
    }

    public List<ItemDto> searchAvailableItems(String text) {
        if (text == null || text.isBlank()) return List.of();

        List<Item> items = itemRepository
                .findByAvailableTrueAndNameContainingIgnoreCaseOrAvailableTrueAndDescriptionContainingIgnoreCase(text, text);

        return items.stream()
                .map(ItemMapper::toItemDto)
                .collect(Collectors.toList());
    }

    public List<ItemBookingDto> getAllItemsWithBookings(Long ownerId) {
        List<Item> items = itemRepository.findByOwnerId(ownerId);
        if (items.isEmpty()) return List.of();

        LocalDateTime now = LocalDateTime.now();
        Map<Long, List<Booking>> bookingsByItem = bookingService.getBookingsForItems(items);

        return items.stream()
                .map(item -> {
                    List<Booking> bookings = bookingsByItem.getOrDefault(item.getId(), List.of());

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

                    return ItemMapper.toItemBookingDto(item, lastBooking, nextBooking);
                })
                .collect(Collectors.toList());
    }

    public CommentDto addComment(Long userId, Long itemId, CommentDto commentDto) {
        User author = userService.findUserEntityById(userId);

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь не найдена"));

        boolean hasBooking = bookingService.hasPastBooking(userId, itemId);

        if (!hasBooking) {
            throw new ValidationException("Пользователь не может оставлять комментарий без бронирования вещи");
        }

        Comment comment = CommentMapper.toComment(commentDto, item, author);
        Comment savedComment = commentService.saveComment(comment);

        return CommentMapper.toCommentDto(savedComment);

    }

    public ItemWithCommentsDto findItemWithComments(Long itemId, Long requesterId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь с id " + itemId + " не найдена"));

        LocalDateTime lastBooking = null;
        LocalDateTime nextBooking = null;

        if (item.getOwner().getId().equals(requesterId)) {
            BookingDto bookings = bookingService.getLastAndNextBookingForItem(item);
            lastBooking = bookings.getStart();
            nextBooking = bookings.getEnd();
        }

        List<CommentDto> comments = commentService.getCommentsForItem(itemId);

        return ItemMapper.toItemWithCommentsDto(item, lastBooking, nextBooking, comments);
    }


}
