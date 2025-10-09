package ru.practicum.shareit.item.repository;

import ru.practicum.shareit.item.model.Item;

import java.util.List;

public interface ItemRepository {
    Item create(Item item);

    Item update(Item item);

    List<Item> getAllItems();

    Item findItem(Long itemId);

    void deleteItemById(Long itemId);

    List<Item> getAllItemsByOwner(Long ownerId);

    List<Item> searchAvailableItems(String text);
}
