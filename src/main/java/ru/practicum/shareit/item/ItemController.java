package ru.practicum.shareit.item;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemBookingDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemWithCommentsDto;

import java.util.Collections;
import java.util.List;

import static ru.practicum.shareit.utility.HeaderConstants.SHARER_USER_HEADER;


@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;


    @GetMapping
    public List<ItemBookingDto> getAllItemsByUser(@RequestHeader(SHARER_USER_HEADER) Long userId) {
        return itemService.getAllItemsWithBookings(userId);
    }


    @GetMapping("/{id}")
    public ItemWithCommentsDto getItemById(@RequestHeader(SHARER_USER_HEADER) Long userId,
                                           @PathVariable Long id) {
        return itemService.findItemWithComments(id, userId);
    }


    @GetMapping("/search")
    public List<ItemDto> searchAvialableItems(@RequestParam String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        return itemService.searchAvailableItems(text);
    }


    @PostMapping
    public ItemDto createItem(@RequestHeader(SHARER_USER_HEADER) Long userId,
                              @RequestBody @Valid ItemDto itemDto) {
        return itemService.createItem(userId, itemDto);
    }

    @PatchMapping("/{itemId}")
    public ItemDto updateItem(@RequestHeader(SHARER_USER_HEADER) Long userId,
                              @PathVariable Long itemId,
                              @RequestBody ItemDto itemDto) {
        return itemService.updateItem(userId, itemId, itemDto);
    }

    @DeleteMapping("/{id}")
    public void deleteItem(@PathVariable Long id) {
        itemService.deleteItemById(id);
    }

    @PostMapping("/{itemId}/comment")
    public CommentDto addComment(@RequestHeader(SHARER_USER_HEADER) Long userId,
                                 @PathVariable Long itemId,
                                 @RequestBody @Valid CommentDto commentDto) {
        return itemService.addComment(userId, itemId, commentDto);
    }


}
