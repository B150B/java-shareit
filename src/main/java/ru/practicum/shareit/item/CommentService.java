package ru.practicum.shareit.item;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.CommentMapper;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.repository.CommentRepository;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class CommentService {
    private final CommentRepository commentRepository;

    public List<CommentDto> getCommentsForItem(Long itemId) {
        return commentRepository.findByItemId(itemId).stream()
                .map(CommentMapper::toCommentDto)
                .collect(Collectors.toList());
    }

    public Comment saveComment(Comment comment) {
        return commentRepository.save(comment);
    }
}
