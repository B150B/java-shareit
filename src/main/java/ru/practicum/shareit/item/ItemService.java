package ru.practicum.shareit.item;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.AccessError;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemMapper;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.dto.UserMapper;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ItemService {
    private final ItemRepository itemRepository;
    private final UserService userService;

    @Autowired
    public ItemService(ItemRepository itemRepository, UserService userService) {
        this.itemRepository = itemRepository;
        this.userService = userService;
    }

    public ItemDto createItem(Long userId, ItemDto itemDto) {
        if (userService.findUserById(userId) == null) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }
        Item item = ItemMapper.toItem(itemDto);
        item.setOwner(UserMapper.toUser(userService.findUserById(userId)));
        return ItemMapper.toItemDto(itemRepository.create(item));
    }

    public List<ItemDto> getAllItemsByOwner(Long ownerId) {
        return itemRepository.getAllItemsByOwner(ownerId).stream()
                .map(ItemMapper::toItemDto)
                .collect(Collectors.toList());
    }

    public ItemDto updateItem(Long userId, Long itemId, ItemDto itemDto) {
        Item existingItem = itemRepository.findItem(itemId);

        if (!userId.equals(existingItem.getOwner().getId())) {
            throw new AccessError("Редактировать вещь может только её пользователь");
        }

        ItemMapper.updateItemFromDto(itemDto, existingItem);

        return ItemMapper.toItemDto(itemRepository.update(existingItem));
    }

    public List<Item> getAllItems() {
        return itemRepository.getAllItems();
    }

    public ItemDto findItemById(Long itemId) {
        return ItemMapper.toItemDto(itemRepository.findItem(itemId));
    }

    public void deleteItemById(Long itemId) {
        itemRepository.deleteItemById(itemId);
    }

    public List<ItemDto> searchAvailableItems(String text) {
        return itemRepository.searchAvailableItems(text).stream()
                .map(ItemMapper::toItemDto)
                .collect(Collectors.toList());
    }

}
