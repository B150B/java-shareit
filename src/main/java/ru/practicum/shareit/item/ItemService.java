package ru.practicum.shareit.item;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.exception.AccessError;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.*;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.CommentRepository;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ItemService {
    private final ItemRepository itemRepository;
    private final UserService userService;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;

    @Autowired
    public ItemService(ItemRepository itemRepository, UserService userService, BookingRepository bookingRepository, UserRepository userRepository, CommentRepository commentRepository) {
        this.itemRepository = itemRepository;
        this.userService = userService;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
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
        return items.stream().map(item -> {

            List<Booking> bookings = bookingRepository.findByItemIdOrderByStartAsc(item.getId());

            LocalDateTime now = LocalDateTime.now();


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
        }).collect(Collectors.toList());
    }

    public CommentDto addComment(Long userId, Long itemId, CommentDto commentDto) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь не найдена"));

        boolean hasBooking = bookingRepository.existsByBooker_IdAndItem_IdAndEndBefore(
                userId, itemId, LocalDateTime.now()
        );

        if (!hasBooking) {
            throw new ValidationException("Пользователь не может оставлять комментарий без бронирования вещи");
        }

        Comment comment = new Comment();
        comment.setText(commentDto.getText());
        comment.setItem(item);
        comment.setAuthor(userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден")));
        comment.setCreated(LocalDateTime.now());

        Comment savedComment = commentRepository.save(comment);

        return CommentMapper.toCommentDto(savedComment);

    }

    public ItemWithCommentsDto findItemWithComments(Long itemId, Long requesterId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь с id " + itemId + " не найдена"));

        List<Booking> bookings = bookingRepository.findByItemIdOrderByStartAsc(item.getId());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastBooking = null;
        LocalDateTime nextBooking = null;


        if (item.getOwner().getId().equals(requesterId)) {
            lastBooking = bookings.stream()
                    .filter(b -> b.getEnd().isBefore(now) && b.getStatus() == BookingStatus.APPROVED)
                    .map(Booking::getEnd)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);

            nextBooking = bookings.stream()
                    .filter(b -> b.getStart().isAfter(now) && b.getStatus() == BookingStatus.APPROVED)
                    .map(Booking::getStart)
                    .min(LocalDateTime::compareTo)
                    .orElse(null);
        }


        List<CommentDto> comments = commentRepository.findByItemId(itemId).stream()
                .map(CommentMapper::toCommentDto)
                .collect(Collectors.toList());

        return ItemMapper.toItemWithCommentsDto(item, lastBooking, nextBooking, comments);
    }


}
