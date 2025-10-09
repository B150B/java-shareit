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

import java.util.List;

@Service
public class ItemService {
    private final ItemRepository itemRepository;
    private final UserService userService;

    @Autowired
    public ItemService(ItemRepository itemRepository, UserService userService) {
        this.itemRepository = itemRepository;
        this.userService = userService;
    }

    public Item createItem(Long userId, ItemDto itemDto) {
        if (userService.findUserById(userId) == null) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }
        Item item = ItemMapper.toItem(itemDto);
        item.setOwner(userService.findUserById(userId));
        return itemRepository.create(item);
    }

    public List<Item> getAllItemsByOwner(Long ownerId) {
        return itemRepository.getAllItemsByOwner(ownerId);
    }

    public Item updateItem(Long userId, Long itemId, ItemDto itemDto) {
        Item existingItem = itemRepository.findItem(itemId);

        if (!userId.equals(existingItem.getOwner().getId())) {
            throw new AccessError("Редактировать вещь может только её пользователь");
        }

        if (itemDto.getName() != null) {
            existingItem.setName(itemDto.getName());
        }
        if (itemDto.getDescription() != null) {
            existingItem.setDescription(itemDto.getDescription());
        }
        if (itemDto.getAvailable() != null) {
            existingItem.setAvailable(itemDto.getAvailable());
        }

        return itemRepository.update(existingItem);
    }

    public List<Item> getAllItems() {
        return itemRepository.getAllItems();
    }

    public Item findItemById(Long itemId) {
        return itemRepository.findItem(itemId);
    }

    public void deleteItemById(Long itemId) {
        itemRepository.deleteItemById(itemId);
    }

    public List<Item> searchAvailableItems(String text) {
        return itemRepository.searchAvailableItems(text);
    }

}
