package com.bankease.account.service;

import com.bankease.account.dto.*;
import com.bankease.account.entity.KycDocument;
import com.bankease.account.entity.User;
import com.bankease.account.repository.KycDocumentRepository;
import com.bankease.account.repository.UserRepository;
import com.bankease.common.exception.BusinessException;
import com.bankease.common.exception.ResourceNotFoundException;
import com.bankease.common.security.JwtTokenProvider;
import com.bankease.common.util.AccountNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final KycDocumentRepository kycDocumentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already registered", "EMAIL_EXISTS");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException("Phone number already registered", "PHONE_EXISTS");
        }

        String accountNumber;
        do {
            accountNumber = AccountNumberGenerator.generate();
        } while (userRepository.existsByAccountNumber(accountNumber));

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .accountNumber(accountNumber)
                .accountType(request.getAccountType())
                .status(User.AccountStatus.ACTIVE)
                .role(User.Role.ROLE_USER)
                .kycVerified(true)
                .balance(new java.math.BigDecimal("10000.00")) // default opening balance
                .dailyTransactionUsed(java.math.BigDecimal.ZERO)
                .dailyTransactionLimit(new java.math.BigDecimal("100000.00")) // 1 Lakh limit
                .build();

        user = userRepository.save(user);
        log.info("New user registered with account number: {}", accountNumber);

        // Publish registration event (async notification)
        eventPublisher.publishEvent(new com.bankease.account.event.UserRegisteredEvent(this, user));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtTokenProvider.generateToken(userDetails, user.getId(), user.getAccountNumber());
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400L)
                .user(mapToUserResponse(user))
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        userRepository.updateLastLoginTime(user.getId(), LocalDateTime.now());

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtTokenProvider.generateToken(userDetails, user.getId(), user.getAccountNumber());
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        log.info("User logged in: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400L)
                .user(mapToUserResponse(user))
                .build();
    }

    @Transactional
    public String submitKyc(Long userId, KycRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (user.getKycVerified()) {
            throw new BusinessException("KYC is already verified", "KYC_ALREADY_VERIFIED");
        }

        if (kycDocumentRepository.existsByDocumentNumber(request.getDocumentNumber())) {
            throw new BusinessException("Document number already exists", "DOCUMENT_EXISTS");
        }

        KycDocument kyc = KycDocument.builder()
                .user(user)
                .documentType(request.getDocumentType())
                .documentNumber(request.getDocumentNumber())
                .documentImageUrl(request.getDocumentImageUrl())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .address(request.getAddress())
                .pinCode(request.getPinCode())
                .city(request.getCity())
                .state(request.getState())
                .status(KycDocument.KycStatus.PENDING)
                .build();

        kycDocumentRepository.save(kyc);
        log.info("KYC submitted for user: {}", userId);

        // Publish KYC event
        eventPublisher.publishEvent(new com.bankease.account.event.KycSubmittedEvent(this, user));

        return "KYC submitted successfully. Verification in progress.";
    }

    @Transactional
    public String approveKyc(Long userId, String approvedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        KycDocument kyc = kycDocumentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("KYC document not found for user: " + userId));

        kyc.setStatus(KycDocument.KycStatus.APPROVED);
        kyc.setVerifiedAt(LocalDateTime.now());
        kyc.setVerifiedBy(approvedBy);
        kycDocumentRepository.save(kyc);

        user.setKycVerified(true);
        user.setStatus(User.AccountStatus.ACTIVE);
        userRepository.save(user);

        log.info("KYC approved for user: {} by: {}", userId, approvedBy);
        eventPublisher.publishEvent(new com.bankease.account.event.KycApprovedEvent(this, user));

        return "KYC approved successfully. Account is now active.";
    }

    @Transactional(readOnly = true)
    public UserResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return mapToUserResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByAccountNumber(String accountNumber) {
        User user = userRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User", "accountNumber", accountNumber));
        return mapToUserResponse(user);
    }

    public UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .accountNumber(user.getAccountNumber())
                .balance(user.getBalance())
                .accountType(user.getAccountType())
                .status(user.getStatus())
                .kycVerified(user.getKycVerified())
                .dailyTransactionLimit(user.getDailyTransactionLimit())
                .dailyTransactionUsed(user.getDailyTransactionUsed())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
