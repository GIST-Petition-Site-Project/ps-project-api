package com.example.gistcompetitioncnserver.post;

import com.example.gistcompetitioncnserver.exception.CustomException;
import com.example.gistcompetitioncnserver.exception.ErrorCase;
import com.example.gistcompetitioncnserver.user.User;
import com.example.gistcompetitioncnserver.user.UserService;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@RequestMapping("/gistps/api/v1") // not used in rest api like v1 url
public class PostController {

    private final PostService postService;
    private final UserService userService;

    private boolean isRequestBodyValid(PostRequestDto postRequestDto) {
        return postRequestDto.getTitle() != null &&
                postRequestDto.getDescription() != null &&
                postRequestDto.getCategory() != null;
    }

    //게시글 작성 요청 보냈을 때 정상적으로 게시글이 생성되면 리턴값으로 작성된 게시글 고유 id 반환해주기
    @PostMapping("/post")
    public ResponseEntity<Object> createPost(@RequestBody PostRequestDto postRequestDto,
                                             @AuthenticationPrincipal String email) {
        User user = userService.findUserByEmail2(email);

        if (!isRequestBodyValid(postRequestDto)) {
            throw new CustomException(ErrorCase.INVAILD_FILED_ERROR);
        }

        return ResponseEntity.created(URI.create("/post/" + postService.createPost(postRequestDto, user.getId())))
                .build();
    }

    @GetMapping("/my-post/{userEmail}")
    public ResponseEntity<Object> retrievePostsByUserId(@PathVariable String userEmail,
                                                        @AuthenticationPrincipal String requestEmail) {
        User user = userService.findUserByEmail2(userEmail);

        if (!requestEmail.equals(userEmail)) {
            throw new CustomException(ErrorCase.BAD_REQUEST_ERROR);
        }

        return ResponseEntity.ok().body(postService.retrievePostsByUserId(user.getId()));
    }

    @GetMapping("/post")
    public ResponseEntity<Object> retrieveAllPost() {
        return ResponseEntity.ok().body(postService.retrieveAllPost());
    }

    @GetMapping("/post/{id}")
    public ResponseEntity<Object> retrievePost(@PathVariable Long id) {
        return ResponseEntity.ok().body(postService.retrievePost(id));
    }

    @GetMapping("/post/count")
    public ResponseEntity<Object> getPageNumber() {
        return ResponseEntity.ok().body(postService.getPageNumber());
    }

    @GetMapping("/post/category")
    public ResponseEntity<Object> getPostsByCategory(@RequestParam("categoryName") String categoryName) {
        return ResponseEntity.ok().body(postService.getPostsByCategory(categoryName));
    }

    @DeleteMapping("/post/{id}") // like도 지워야함
    public ResponseEntity<Object> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/post/{postId}/like")
    public ResponseEntity<Object> LikePost(@PathVariable Long postId, @AuthenticationPrincipal String email) {
        User user = userService.findUserByEmail2(email);

        return ResponseEntity
                .ok()
                .body(Boolean.toString(postService.like(postId, user.getId())));
    }
}
