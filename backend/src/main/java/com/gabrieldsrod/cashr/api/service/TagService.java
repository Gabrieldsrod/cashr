package com.gabrieldsrod.cashr.api.service;

import com.gabrieldsrod.cashr.api.dto.request.TagRequest;
import com.gabrieldsrod.cashr.api.dto.response.TagResponse;
import com.gabrieldsrod.cashr.api.exception.BusinessException;
import com.gabrieldsrod.cashr.api.model.Tag;
import com.gabrieldsrod.cashr.api.model.Transaction;
import com.gabrieldsrod.cashr.api.model.User;
import com.gabrieldsrod.cashr.api.repository.TagRepository;
import com.gabrieldsrod.cashr.api.repository.TransactionRepository;
import com.gabrieldsrod.cashr.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public List<TagResponse> findAll(UUID userId) {
        return tagRepository.findAllByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TagResponse create(TagRequest request, UUID userId) {
        tagRepository.findByUserIdAndNameIgnoreCase(userId, request.getName())
                .ifPresent(t -> { throw new BusinessException("Tag '" + request.getName() + "' already exists"); });

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        return toResponse(tagRepository.save(Tag.builder().user(user).name(request.getName()).build()));
    }

    @Transactional
    public void delete(UUID id) {
        tagRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Tag not found"));
        tagRepository.deleteById(id);
    }

    @Transactional
    public void attachToTransaction(UUID transactionId, List<String> tagNames) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new BusinessException("Transaction not found"));

        UUID userId = transaction.getUserId();
        tagNames.forEach(name -> transaction.getTags().add(findOrCreate(name, userId)));

        transactionRepository.save(transaction);
    }

    @Transactional
    public void detachFromTransaction(UUID transactionId, UUID tagId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new BusinessException("Transaction not found"));

        transaction.getTags().removeIf(tag -> tag.getId().equals(tagId));
        transactionRepository.save(transaction);
    }

    private Tag findOrCreate(String name, UUID userId) {
        return tagRepository.findByUserIdAndNameIgnoreCase(userId, name)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new BusinessException("User not found"));
                    return tagRepository.save(Tag.builder().user(user).name(name).build());
                });
    }

    TagResponse toResponse(Tag tag) {
        return TagResponse.builder()
                .id(tag.getId())
                .userId(tag.getUser().getId())
                .name(tag.getName())
                .build();
    }
}
