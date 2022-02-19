package com.gistpetition.api.petition.dto;

import com.gistpetition.api.petition.domain.Petition;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;

@Data
public class PetitionPreviewResponse {
    private final Long id;
    private final String title;
    private final String categoryName;
    private final Integer agreements;
    private final LocalDateTime createdAt;
    private final String tempUrl;
    private final Boolean expired;

    public static PetitionPreviewResponse of(Petition petition) {
        return new PetitionPreviewResponse(
                petition.getId(),
                petition.getTitle(),
                petition.getCategory().getName(),
                petition.getAgreeCount(),
                petition.getCreatedAt(),
                petition.getTempUrl(),
                petition.isExpiredAt(LocalDateTime.now())
        );
    }

    public static Page<PetitionPreviewResponse> pageOf(Page<Petition> page) {
        return page.map(PetitionPreviewResponse::of);
    }
}
