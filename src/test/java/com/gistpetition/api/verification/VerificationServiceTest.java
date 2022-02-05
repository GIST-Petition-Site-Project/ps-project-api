package com.gistpetition.api.verification;

import com.gistpetition.api.ServiceTest;
import com.gistpetition.api.exception.user.DuplicatedUserException;
import com.gistpetition.api.exception.user.InvalidEmailFormException;
import com.gistpetition.api.exception.verification.DuplicatedVerificationException;
import com.gistpetition.api.exception.verification.ExpiredVerificationCodeException;
import com.gistpetition.api.exception.verification.NoSuchVerificationInfoException;
import com.gistpetition.api.user.domain.User;
import com.gistpetition.api.user.domain.UserRepository;
import com.gistpetition.api.user.domain.UserRole;
import com.gistpetition.api.verification.application.VerificationService;
import com.gistpetition.api.verification.domain.VerificationInfo;
import com.gistpetition.api.verification.domain.VerificationInfoRepository;
import com.gistpetition.api.verification.dto.UsernameConfirmationRequest;
import com.gistpetition.api.verification.dto.VerificationEmailRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class VerificationServiceTest extends ServiceTest {

    private static final String GIST_EMAIL = "tester@gist.ac.kr";
    private static final String PASSWORD = "password!";
    private static final String VERIFICATION_CODE = "AAAAAA";

    @Autowired
    private VerificationService verificationService;
    @Autowired
    private VerificationInfoRepository verificationInfoRepository;
    @Autowired
    private UserRepository userRepository;

    @Test
    void createVerificationCode() {
        String verificationCode = verificationService.createVerificationInfo(new VerificationEmailRequest(GIST_EMAIL));
        VerificationInfo verificationInfo = verificationInfoRepository.findByVerificationCode(verificationCode).orElseThrow(IllegalArgumentException::new);

        assertThat(verificationInfo.getVerificationCode()).isEqualTo(verificationCode);
        assertThat(verificationInfo.getUsername()).isEqualTo(GIST_EMAIL);
        assertThat(verificationInfo.getCreatedAt()).isNotNull();
        assertThat(verificationInfo.getConfirmedAt()).isNull();
    }

    @Test
    void createVerificationCodeFailedIfAlreadyExisted() {
        userRepository.save(new User(GIST_EMAIL, PASSWORD, UserRole.USER, true));
        assertThatThrownBy(
                () -> verificationService.createVerificationInfo(new VerificationEmailRequest(GIST_EMAIL))
        ).isInstanceOf(DuplicatedUserException.class);
    }

    @Test
    void createVerificationCodeFailedIfNotValidEmailForm() {
        String notGistEmail = "notGistEmail@gmail.com";
        assertThatThrownBy(
                () -> verificationService.createVerificationInfo(new VerificationEmailRequest(notGistEmail))
        ).isInstanceOf(InvalidEmailFormException.class);
    }

    @Test
    void createVerificationCodeIfVerificationCodeExist() {
        verificationInfoRepository.save(new VerificationInfo(GIST_EMAIL, "BBBBBB"));

        String code = verificationService.createVerificationInfo(new VerificationEmailRequest(GIST_EMAIL));

        List<VerificationInfo> infos = verificationInfoRepository.findByUsername(GIST_EMAIL);
        assertThat(infos).hasSize(1);
        assertThat(infos.get(0).getVerificationCode()).isEqualTo(code);
    }

    @Test
    void confirmVerificationCode() {
        verificationInfoRepository.save(new VerificationInfo(null, GIST_EMAIL, VERIFICATION_CODE, LocalDateTime.now(), null));

        verificationService.confirmUsername(new UsernameConfirmationRequest(GIST_EMAIL, VERIFICATION_CODE));

        VerificationInfo verificationInfo = verificationInfoRepository.findByVerificationCode(VERIFICATION_CODE).orElseThrow(IllegalArgumentException::new);
        assertThat(verificationInfo.getConfirmedAt()).isNotNull();
    }

    @Test
    void confirmVerificationCodeNotExisting() {
        verificationInfoRepository.save(new VerificationInfo(null, GIST_EMAIL, VERIFICATION_CODE, LocalDateTime.now(), null));

        String incorrectVerificationCode = VERIFICATION_CODE + "A";
        UsernameConfirmationRequest requestWithIncorrectCode = new UsernameConfirmationRequest(GIST_EMAIL, incorrectVerificationCode);
        assertThatThrownBy(
                () -> verificationService.confirmUsername(requestWithIncorrectCode)
        ).isInstanceOf(NoSuchVerificationInfoException.class);
    }

    @Test
    void confirmVerificationCodeWhenExpired() {
        LocalDateTime expiredCreatedTime = LocalDateTime.now().minusMinutes(VerificationInfo.CONFIRM_EXPIRE_MINUTE + 1);
        verificationInfoRepository.save(new VerificationInfo(null, GIST_EMAIL, VERIFICATION_CODE, expiredCreatedTime, null));

        UsernameConfirmationRequest expiredInfoRequest = new UsernameConfirmationRequest(GIST_EMAIL, VERIFICATION_CODE);
        assertThatThrownBy(
                () -> verificationService.confirmUsername(expiredInfoRequest)
        ).isInstanceOf(ExpiredVerificationCodeException.class);
    }

    @Test
    void confirmVerificationAlreadyConfirmedVerification() {
        verificationInfoRepository.save(new VerificationInfo(null, GIST_EMAIL, VERIFICATION_CODE, LocalDateTime.now().minusMinutes(1), LocalDateTime.now()));
        assertThatThrownBy(
                () -> verificationService.confirmUsername(new UsernameConfirmationRequest(GIST_EMAIL, VERIFICATION_CODE))
        ).isInstanceOf(DuplicatedVerificationException.class);
    }

    @AfterEach
    void tearDown() {
        verificationInfoRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }
}