package com.example.gistcompetitioncnserver.verification;

import com.example.gistcompetitioncnserver.exception.CustomException;
import com.example.gistcompetitioncnserver.user.User;
import com.example.gistcompetitioncnserver.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class VerificationService {

    private final VerificationTokenRepository verificationTokenRepository;
    private final VerificationTokenRepository2 verificationTokenRepository2;
    private final TokenGenerator tokenGenerator;
    private final UserRepository userRepository;


    public VerificationService(VerificationTokenRepository verificationTokenRepository, VerificationTokenRepository2 verificationTokenRepository2, TokenGenerator tokenGenerator, UserRepository userRepository) {
        this.verificationTokenRepository = verificationTokenRepository;
        this.verificationTokenRepository2 = verificationTokenRepository2;
        this.tokenGenerator = tokenGenerator;
        this.userRepository = userRepository;
    }

    @Transactional
    public String createToken(User user) {
        String token = UUID.randomUUID().toString();
        verificationTokenRepository.save(new VerificationToken(token, user.getId()));
        return token;
    }

    @Transactional
    public void confirm(String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new CustomException("존재하지 않는 토큰입니다."));

        if (!verificationToken.isValidAt(LocalDateTime.now())) {
            throw new CustomException("만료된 토큰입니다.");
        }

        User user = userRepository.findById(verificationToken.getUserId()).orElseThrow(() -> new CustomException("존재하지 않는 사용자입니다."));
        if (user.isEnabled()) {
            throw new CustomException("이미 인증된 사용자입니다.");
        }
        user.setEnabled();
    }

    @Transactional
    public String createToken2(VerificationEmailRequest request) {
        String token = tokenGenerator.createToken();
        verificationTokenRepository2.save(new VerificationToken2(request.getEmail(), token));
        return token;
    }
}
