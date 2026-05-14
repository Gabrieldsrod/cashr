package com.gabrieldsrod.cashr.api.service;

import com.gabrieldsrod.cashr.api.dto.request.AuthLoginRequest;
import com.gabrieldsrod.cashr.api.dto.request.AuthRegisterRequest;
import com.gabrieldsrod.cashr.api.dto.response.AuthTokenResponse;
import com.gabrieldsrod.cashr.api.model.User;
import com.gabrieldsrod.cashr.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthTokenResponse register(AuthRegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email já cadastrado");
        }

        User user = User.builder()
                .email(request.getEmail())
                .fullName(request.getFullName())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();

        user = userRepository.save(user);

        String token = tokenProvider.generateToken(user.getId(), user.getEmail());
        return AuthTokenResponse.builder()
                .token(token)
                .userId(user.getId().toString())
                .email(user.getEmail())
                .build();
    }

    public AuthTokenResponse login(AuthLoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Senha incorreta");
        }

        String token = tokenProvider.generateToken(user.getId(), user.getEmail());
        return AuthTokenResponse.builder()
                .token(token)
                .userId(user.getId().toString())
                .email(user.getEmail())
                .build();
    }
}
