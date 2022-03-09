package com.gistpetition.api.unit.petition;

import com.gistpetition.api.common.PetitionBuilder;
import com.gistpetition.api.exception.petition.AlreadyReleasedPetitionException;
import com.gistpetition.api.exception.petition.ExpiredPetitionException;
import com.gistpetition.api.exception.petition.NotEnoughAgreementException;
import com.gistpetition.api.exception.petition.NotReleasedPetitionException;
import com.gistpetition.api.petition.domain.Petition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.stream.LongStream;

import static com.gistpetition.api.petition.domain.Petition.REQUIRED_AGREEMENT_FOR_RELEASE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PetitionTest {
    public static final Instant PETITION_CREATION_AT = LocalDateTime.of(2002, 2, 2, 2, 2).toInstant(ZoneOffset.UTC);
    public static final Instant PETITION_ONGOING_AT = PETITION_CREATION_AT.plusSeconds(Petition.POSTING_PERIOD_BY_SECONDS / 2);
    public static final Instant PETITION_EXPIRED_AT = PETITION_CREATION_AT.plusSeconds(Petition.POSTING_PERIOD_BY_SECONDS);
    private static final String AGREEMENT_DESCRIPTION = "동의합니다.";
    private static final String TEMP_URL = "AAAAAA";

    private Petition petition;

    @BeforeEach
    void setUp() {
        petition = PetitionBuilder.aPetition().withExpiredAt(PETITION_EXPIRED_AT).withTempUrl(TEMP_URL).build();
    }

    @Test
    void agree() {
        petition.agree(1L, AGREEMENT_DESCRIPTION, PETITION_ONGOING_AT);

        assertThat(petition.getAgreeCount()).isEqualTo(1);
    }

    @Test
    void agreeByMultipleUser() {
        petition.agree(1L, AGREEMENT_DESCRIPTION, PETITION_ONGOING_AT);
        petition.agree(2L, AGREEMENT_DESCRIPTION, PETITION_ONGOING_AT);
        petition.agree(3L, AGREEMENT_DESCRIPTION, PETITION_ONGOING_AT);

        assertThat(petition.getAgreeCount()).isEqualTo(3);
    }

    @Test
    void agreeExpiredPetition() {
        assertThatThrownBy(() ->
                petition.agree(1L, AGREEMENT_DESCRIPTION, PETITION_EXPIRED_AT.plusSeconds(1))
        ).isInstanceOf(ExpiredPetitionException.class);
    }

    @Test
    void release() {
        agreePetitionByMultipleUsers(petition, REQUIRED_AGREEMENT_FOR_RELEASE);

        petition.release(PETITION_ONGOING_AT);

        assertTrue(petition.isReleased());
    }

    @Test
    void releaseAlreadyReleased() {
        agreePetitionByMultipleUsers(petition, REQUIRED_AGREEMENT_FOR_RELEASE);
        petition.release(PETITION_ONGOING_AT);

        assertThatThrownBy(
                () -> petition.release(PETITION_ONGOING_AT.plusSeconds(1))
        ).isInstanceOf(AlreadyReleasedPetitionException.class);
    }

    @Test
    void releaseNotEnoughAgreement() {
        assertThatThrownBy(() -> petition.release(PETITION_ONGOING_AT)
        ).isInstanceOf(NotEnoughAgreementException.class);
    }

    @Test
    void releaseExpiredPetition() {
        agreePetitionByMultipleUsers(petition, REQUIRED_AGREEMENT_FOR_RELEASE);

        petition.release(PETITION_ONGOING_AT);

        assertThatThrownBy(() ->
                petition.release(PETITION_EXPIRED_AT.plusSeconds(1))
        ).isInstanceOf(ExpiredPetitionException.class);
    }

    @Test
    void cancelRelease() {
        agreePetitionByMultipleUsers(petition, REQUIRED_AGREEMENT_FOR_RELEASE);
        petition.release(PETITION_ONGOING_AT);

        petition.cancelRelease();

        assertFalse(petition.isReleased());
    }

    @Test
    void cancelReleaseIfNotReleased() {
        assertThatThrownBy(
                () -> petition.cancelRelease()
        ).isInstanceOf(NotReleasedPetitionException.class);
    }

    private void agreePetitionByMultipleUsers(Petition target, int numberOfUsers) {
        LongStream.range(0, numberOfUsers)
                .forEach(userId -> target.agree(userId, AGREEMENT_DESCRIPTION, PETITION_ONGOING_AT));
    }
}