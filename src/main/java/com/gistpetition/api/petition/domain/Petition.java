package com.gistpetition.api.petition.domain;

import com.gistpetition.api.common.persistence.BaseEntity;
import com.gistpetition.api.exception.petition.*;
import com.gistpetition.api.user.domain.User;
import lombok.Getter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import javax.persistence.*;
import java.time.Instant;

@Audited
@Getter
@Entity
public class Petition extends BaseEntity {
    public static final int REQUIRED_AGREEMENT_FOR_RELEASE = 5;
    public static final int REQUIRED_AGREEMENT_FOR_ANSWER = 20;
    public static final int POSTING_PERIOD_BY_SECONDS = 30 * 24 * 60 * 60;

    private String title;
    @Lob
    private String description;
    @Enumerated(EnumType.STRING)
    private Category category;
    private Boolean released = false;
    private Boolean answered = false;
    private Instant expiredAt;
    private Long userId;
    @Column(unique = true)
    private String tempUrl;
    @NotAudited
    private Integer agreeCount = 0;
    @NotAudited
    @Embedded
    private final Agreements agreements = new Agreements();
    @NotAudited
    @OneToOne(mappedBy = "petition", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST, orphanRemoval = true)
    private Answer answer;

    protected Petition() {
    }

    public Petition(String title, String description, Category category, Instant expiredAt, Long userId, String tempUrl) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.expiredAt = expiredAt;
        this.userId = userId;
        this.tempUrl = tempUrl;
    }

    public void agree(Long userId, String description, Instant at) {
        if (isExpiredAt(at)) {
            throw new ExpiredPetitionException();
        }
        this.agreements.add(new Agreement(description, userId, this));
        this.agreeCount += 1;
    }

    public boolean isAgreedBy(User user) {
        return agreements.isAgreedBy(user.getId());
    }

    public void release(Instant at) {
        if (isExpiredAt(at)) {
            throw new ExpiredPetitionException();
        }
        if (released) {
            throw new AlreadyReleasedPetitionException();
        }
        if (agreeCount < REQUIRED_AGREEMENT_FOR_RELEASE) {
            throw new NotEnoughAgreementException();
        }
        this.released = true;
    }

    public void cancelRelease() {
        if (!released) {
            throw new NotReleasedPetitionException();
        }
        this.released = false;
    }

    public void answer(String content) {
        if (!released) {
            throw new NotReleasedPetitionException();
        }
        if (agreeCount < REQUIRED_AGREEMENT_FOR_ANSWER) {
            throw new NotEnoughAgreementException();
        }
        this.answer = new Answer(content, this);
        this.answered = true;
    }

    public void updateAnswer(String updateAnswerContent) {
        if (!isAnswered()) {
            throw new NotAnsweredPetitionException();
        }
        this.answer.updateContent(updateAnswerContent);
    }

    public void deleteAnswer() {
        if (!isAnswered()) {
            throw new NotAnsweredPetitionException();
        }
        answer.detach();
        this.answer = null;
        this.answered = false;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isReleased() {
        return released;
    }

    public boolean isAnswered() {
        return answered;
    }

    public boolean isExpiredAt(Instant time) {
        return time.isAfter(expiredAt);
    }
}
