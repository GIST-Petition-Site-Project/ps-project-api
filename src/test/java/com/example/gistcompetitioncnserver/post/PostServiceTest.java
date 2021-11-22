package com.example.gistcompetitioncnserver.post;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.gistcompetitioncnserver.user.User;
import com.example.gistcompetitioncnserver.user.UserRepository;
import com.example.gistcompetitioncnserver.user.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles(profiles = "test")
public class PostServiceTest {
    @Autowired
    private PostService postService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PostRepository postRepository;

    @Test
    void like() {
        User user = userRepository.save(new User("userName", "email", "password", UserRole.USER));
        Long postId = postService.createPost(
                new PostRequestDto("title", "description", "category", user.getId()), user.getId());
        Post post = postRepository.findById(postId).orElseThrow(IllegalArgumentException::new);
        assertThat(post.getLikes()).hasSize(0);

        postService.like(postId, user.getId());
        post = postRepository.findById(postId).orElseThrow(IllegalArgumentException::new);
        assertThat(post.getLikes()).hasSize(1);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
        postRepository.deleteAllInBatch();
    }
}