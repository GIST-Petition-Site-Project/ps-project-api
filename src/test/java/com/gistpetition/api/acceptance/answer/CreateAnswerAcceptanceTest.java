package com.gistpetition.api.acceptance.answer;

import com.gistpetition.api.acceptance.AcceptanceTest;
import com.gistpetition.api.acceptance.common.TUser;
import com.gistpetition.api.answer.domain.AnswerRepository;
import com.gistpetition.api.answer.dto.AnswerRequest;
import com.gistpetition.api.exception.petition.DuplicatedAnswerException;
import com.gistpetition.api.petition.domain.Category;
import com.gistpetition.api.petition.domain.PetitionRepository;
import com.gistpetition.api.petition.dto.PetitionRequest;
import com.gistpetition.api.user.domain.UserRepository;
import com.gistpetition.api.user.domain.UserRole;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.gistpetition.api.acceptance.common.TUser.GUNE;
import static com.gistpetition.api.acceptance.common.TUser.T_ADMIN;
import static org.assertj.core.api.Assertions.assertThat;

public class CreateAnswerAcceptanceTest extends AcceptanceTest {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PetitionRepository petitionRepository;
    @Autowired
    private AnswerRepository answerRepository;

    @Test
    public void createAnswerWithConcurrency() throws InterruptedException {
        PetitionRequest petitionRequest = new PetitionRequest("title", "description", Category.ACADEMIC.getId());
        AnswerRequest answerRequest = new AnswerRequest("contents");

        GUNE.doSignUp();
        Response createPetition = T_ADMIN.doLoginAndThen().updateUserRoleAndThen(GUNE, UserRole.MANAGER).createPetition(petitionRequest);
        assertThat(createPetition.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        String[] locationHeader = createPetition.header(HttpHeaders.LOCATION).split("/");
        Long petitionId = Long.valueOf(locationHeader[locationHeader.length - 1]);

        int numberOfThreads = 10;
        ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        for (int i = 0; i < numberOfThreads; i++) {
            service.execute(() -> {
                try {
                    Response createAnswer = GUNE.doLoginAndThen().createAnswer(petitionId, answerRequest);
                    if (createAnswer.statusCode() != HttpStatus.CREATED.value()) {
                        throw new DuplicatedAnswerException();
                    }
                } catch (DuplicatedAnswerException ex) {
                    System.out.println(Thread.currentThread().getName() + ": " + ex);
                }
                latch.countDown();
            });
        }
        latch.await();
        assertThat(answerRepository.findListByPetitionId(petitionId)).hasSize(1);
    }

    @AfterEach
    void tearDown() {
        TUser.clearAll();
        answerRepository.deleteAllInBatch();
        petitionRepository.deleteAllInBatch();
    }
}
