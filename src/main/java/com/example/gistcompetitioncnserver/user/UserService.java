package com.example.gistcompetitioncnserver.user;

import com.example.gistcompetitioncnserver.exception.user.DuplicatedUserException;
import com.example.gistcompetitioncnserver.exception.user.InvalidEmailFormException;
import com.example.gistcompetitioncnserver.exception.user.NoSuchUserException;
import com.example.gistcompetitioncnserver.exception.user.NotMatchedPasswordException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpSession;
import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final Encryptor encryptor;
    private final HttpSession httpSession;

    public UserService(UserRepository userRepository, Encryptor encryptor, HttpSession httpSession) {
        this.userRepository = userRepository;
        this.encryptor = encryptor;
        this.httpSession = httpSession;
    }

    @Transactional
    public Long signUp(SignUpRequest request) {
        String username = request.getUsername();
        if (userRepository.existsByUsername(username)) {
            throw new DuplicatedUserException();
        }
        if (!EmailDomain.has(EmailParser.parseDomainFrom(username))) {
            throw new InvalidEmailFormException();
        }

        User user = new User(
                username,
                encryptor.hashPassword(request.getPassword()),
                UserRole.USER);
        return userRepository.save(user).getId();
    }

    @Transactional(readOnly = true)
    public void signIn(SignInRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(NoSuchUserException::new);
        if (!encryptor.isMatch(request.getPassword(), user.getPassword())) {
            throw new NotMatchedPasswordException();
        }
        httpSession.setAttribute("user", new SessionUser(user));
    }

    @Transactional(readOnly = true)
    public User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(NoSuchUserException::new);
    }

    @Transactional(readOnly = true)
    public User findUserByEmail(String email) {
        return userRepository.findByUsername(email)
                .orElseThrow(NoSuchUserException::new);
    }

    @Transactional(readOnly = true)
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public void updateUserRole(Long userId, UpdateUserRoleRequest userRoleRequest) {
        User user = findUserById(userId);
        user.setUserRole(UserRole.ignoringCaseValueOf(userRoleRequest.getUserRole()));
    }

    @Transactional
    public void updatePassword(Long userId, UpdatePasswordRequest passwordRequest) {
        User user = findUserById(userId);
        if (!encryptor.isMatch(passwordRequest.getOriginPassword(), user.getPassword())) {
            throw new CustomException("기존 패쓰워드가 일치하지 않습니다.");
        }
        user.setPassword(encryptor.hashPassword(passwordRequest.getNewPassword()));
    }

    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NoSuchUserException();
        }
        userRepository.deleteById(userId);
    }

    @Transactional
    public void deleteUserOfMine(Long userId, DeleteUserRequest deleteUserRequest) {
        User user = findUserById(userId);
        if (!encryptor.isMatch(deleteUserRequest.getPassword(), user.getPassword())) {
            throw new CustomException("기존 패쓰워드가 일치하지 않습니다.");
        }
        userRepository.deleteById(userId);
    }
}
