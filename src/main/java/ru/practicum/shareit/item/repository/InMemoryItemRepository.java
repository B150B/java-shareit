package ru.practicum.shareit.item.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.model.Item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class InMemoryItemRepository implements ItemRepository {

    private final Map<Long, Item> itemMap = new HashMap<>();
    private long nextId = 1L;

    private Long getNextId() {
        return nextId++;
    }

    @Override
    public Item create(Item item) {
        item.setId(getNextId());
        itemMap.put(item.getId(), item);
        log.info("Добавлена вещь {} с id {}", item.getName(), item.getId());
        return item;
    }

    @Override
    public Item update(Item item) {
        Long itemId = item.getId();
        if (!itemMap.containsKey(itemId)) {
            throw new NotFoundException("Вещь с id " + itemId + " не найдена");
        }

        Item updatedItem = itemMap.get(itemId);

        updatedItem.setName(item.getName());
        updatedItem.setDescription(item.getDescription());
        updatedItem.setAvailable(item.getAvailable());

        log.info("Обновлена вещь c id {}", itemId);

        return updatedItem;
    }

    @Override
    public List<Item> getAllItems() {
        log.info("Запрошен список всех предметов");
        return itemMap.values().stream().toList();
    }

    @Override
    public Item findItem(Long itemId) {
        if (!itemMap.containsKey(itemId)) {
            throw new NotFoundException("Вещь с id " + itemId + " не найдена");
        }
        log.info("Поиск предмета с id {}", itemId);
        return itemMap.get(itemId);
    }

    @Override
    public void deleteItemById(Long itemId) {
        Item removedItem = itemMap.remove(itemId);
        if (removedItem == null) {
            throw new NotFoundException("Вещь с id " + itemId + " не найдена");
        }
        log.info("Вещь с id {} удалена", itemId);
    }

    @Override
    public List<Item> getAllItemsByOwner(Long ownerId) {
        log.info("Поиск предметов пользоватля с id {}", ownerId);
        return itemMap.values().stream()
                .filter(item -> item.getOwner().getId().equals(ownerId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Item> searchAvailableItems(String text) {
        if (text == null || text.isBlank()) return List.of();
        String lowerCaseText = text.toLowerCase();
        log.info("Поиск предмета по тексту {}", text);
        return itemMap.values().stream()
                .filter(Item::getAvailable)
                .filter(item -> item.getName().toLowerCase().contains(lowerCaseText)
                        || item.getDescription().toLowerCase().contains(lowerCaseText))
                .collect(Collectors.toList());
    }
}
