package ru.practicum.shareit.item;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemMapper;
import ru.practicum.shareit.item.model.Item;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TODO Sprint add-controllers.
 */
@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    //Некоторые тесты хотят получать почему-то именно Item, а не ItemDto

    @GetMapping
    public List<ItemDto> getAllItemsByUser(@RequestHeader("X-Sharer-User-Id") Long userId) {
        return itemService.getAllItemsByOwner(userId).stream()
                .map(ItemMapper::toItemDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public Item getItemById(@PathVariable Long id) {
        //return ItemMapper.toItemDto(itemService.findItemById(id));
        return itemService.findItemById(id);
    }

    @GetMapping("/search")
    public List<ItemDto> searchAvialableItems(@RequestParam String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        return itemService.searchAvailableItems(text).stream()
                .map(ItemMapper::toItemDto)
                .collect(Collectors.toList());
    }


    @PostMapping
    public Item createItem(@RequestHeader("X-Sharer-User-Id") Long userId,
                           @RequestBody @Valid ItemDto itemDto) {
        return itemService.createItem(userId, itemDto);
    }

    @PatchMapping("/{itemId}")
    public Item updateItem(@RequestHeader("X-Sharer-User-Id") Long userId,
                           @PathVariable Long itemId,
                           @RequestBody ItemDto itemDto) {
        return itemService.updateItem(userId, itemId, itemDto);

    }

    @DeleteMapping("/{id}")
    public void deleteItem(@PathVariable Long id) {
        itemService.deleteItemById(id);
    }


}
