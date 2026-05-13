package com.gabrieldsrod.cashr.api.service;

import com.gabrieldsrod.cashr.api.dto.CreditCardRequest;
import com.gabrieldsrod.cashr.api.dto.CreditCardResponse;
import com.gabrieldsrod.cashr.api.exception.BusinessException;
import com.gabrieldsrod.cashr.api.model.CreditCard;
import com.gabrieldsrod.cashr.api.model.User;
import com.gabrieldsrod.cashr.api.repository.CreditCardRepository;
import com.gabrieldsrod.cashr.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreditCardService {

    private final CreditCardRepository creditCardRepository;
    private final UserRepository userRepository;

    public CreditCardResponse create(CreditCardRequest request) {
        User user = findUser(request.getUserId());

        if (creditCardRepository.existsByNameAndUserId(request.getName(), user.getId())) {
            throw new BusinessException("Credit card with name '" + request.getName() + "' already exists for this user");
        }

        CreditCard creditCard = CreditCard.builder()
                .name(request.getName())
                .bank(request.getBank())
                .closingDay(request.getClosingDay())
                .dueDay(request.getDueDay())
                .creditLimit(request.getCreditLimit())
                .user(user)
                .build();

        return toResponse(creditCardRepository.save(creditCard));
    }

    public CreditCardResponse findById(UUID id) {
        return toResponse(findCreditCard(id));
    }

    public List<CreditCardResponse> findAllByUser(UUID userId) {
        return creditCardRepository.findAllByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public CreditCardResponse update(UUID id, CreditCardRequest request) {
        CreditCard creditCard = findCreditCard(id);
        User user = findUser(request.getUserId());

        boolean nameChanged = !creditCard.getName().equals(request.getName());
        if (nameChanged && creditCardRepository.existsByNameAndUserId(request.getName(), user.getId())) {
            throw new BusinessException("Credit card with name '" + request.getName() + "' already exists for this user");
        }

        creditCard.setName(request.getName());
        creditCard.setBank(request.getBank());
        creditCard.setClosingDay(request.getClosingDay());
        creditCard.setDueDay(request.getDueDay());
        creditCard.setCreditLimit(request.getCreditLimit());
        creditCard.setUser(user);

        return toResponse(creditCardRepository.save(creditCard));
    }

    public void delete(UUID id) {
        findCreditCard(id);
        creditCardRepository.deleteById(id);
    }

    private CreditCard findCreditCard(UUID id) {
        return creditCardRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Credit card not found"));
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));
    }

    private CreditCardResponse toResponse(CreditCard card) {
        return CreditCardResponse.builder()
                .id(card.getId())
                .name(card.getName())
                .bank(card.getBank())
                .closingDay(card.getClosingDay())
                .dueDay(card.getDueDay())
                .creditLimit(card.getCreditLimit())
                .userId(card.getUser().getId())
                .build();
    }
}
