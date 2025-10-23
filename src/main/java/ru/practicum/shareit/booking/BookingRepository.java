package ru.practicum.shareit.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByBookerOrderByStartDesc(User booker);

    List<Booking> findByItemInOrderByStartDesc(List<Item> items);

    List<Booking> findByItemIdOrderByStartAsc(Long itemId);

    boolean existsByBooker_IdAndItem_IdAndEndBefore(Long bookerId, Long itemId, LocalDateTime now);
}
