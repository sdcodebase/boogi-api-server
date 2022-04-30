package boogi.apiserver.domain.user.application;

import boogi.apiserver.domain.user.dao.UserRepository;
import boogi.apiserver.domain.user.domain.User;
import boogi.apiserver.domain.user.dto.UserDetailInfoResponse;
import boogi.apiserver.domain.user.exception.WithdrawnOrCanceledUserException;
import boogi.apiserver.global.error.exception.InvalidValueException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService {

    private final UserRepository userRepository;

    public User getUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(InvalidValueException::new);
        if (user.getCanceledAt() != null) {
            throw new WithdrawnOrCanceledUserException();
        }
        return user;
    }

    public User getUserByEmail(String email) {
        Optional<User> _user = userRepository.findByEmail(email);
        if (_user.isEmpty()) {
            throw new InvalidValueException("해당 이메일은 없는 계정입니다.");
        }
        User user = _user.get();
        if (user.getCanceledAt() != null) {
            throw new WithdrawnOrCanceledUserException();
        }
        return user;
    }

    public UserDetailInfoResponse getUserDetailInfo(Long userId) {
        User user = this.getUser(userId);
        return UserDetailInfoResponse.of(user);
    }
}