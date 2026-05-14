package com.gabrieldsrod.cashr.api.service;

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

    @Transactional
    public Tag findOrCreate(String name, UUID userId) {
        return tagRepository.findByUserIdAndNameIgnoreCase(userId, name)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    return tagRepository.save(Tag.builder().user(user).name(name).build());
                });
    }

    @Transactional
    public void attachToTransaction(UUID transactionId, List<String> tagNames) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        UUID userId = transaction.getUserId();

        tagNames.forEach(name -> {
            Tag tag = findOrCreate(name, userId);
            transaction.getTags().add(tag);
        });

        transactionRepository.save(transaction);
    }

    @Transactional
    public void detachFromTransaction(UUID transactionId, UUID tagId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        transaction.getTags().removeIf(tag -> tag.getId().equals(tagId));

        transactionRepository.save(transaction);
    }
}
