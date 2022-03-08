package com.gistpetition.api.petition.domain;

import com.gistpetition.api.exception.petition.DuplicatedAgreementException;
import org.hibernate.annotations.BatchSize;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Embeddable
public class Agreements {

    @BatchSize(size = 10)
    @OneToMany(mappedBy = "petition", cascade = CascadeType.PERSIST, orphanRemoval = true)
    private final List<Agreement> agreements = new ArrayList<>();

    protected Agreements() {
    }

    public void add(Agreement agreement) {
        if (agreements.contains(agreement)) {
            throw new DuplicatedAgreementException();
        }
        this.agreements.add(agreement);
    }

    public boolean isAgreedBy(Long userId) {
        return agreements.stream().anyMatch(a -> a.writtenBy(userId));
    }

    public List<Agreement> getAgreements() {
        return Collections.unmodifiableList(agreements);
    }
}