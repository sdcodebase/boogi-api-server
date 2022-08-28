package boogi.apiserver.domain.like.dto.response;


import boogi.apiserver.domain.user.domain.User;
import boogi.apiserver.global.dto.PaginationDto;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Slice;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class LikeMembersAtPost {

    private List<userInfo> members = new ArrayList<>();

    private PaginationDto pageInfo;

    @Getter
    @Builder
    public static class userInfo {
        private Long id;
        private String name;
        private String tagNum;
        private String profileImageUrl;

        private static userInfo toDto(User user) {
            return userInfo.builder()
                    .id(user.getId())
                    .name(user.getUsername())
                    .tagNum(user.getTagNumber())
                    .profileImageUrl(user.getProfileImageUrl())
                    .build();
        }
    }

    public LikeMembersAtPost(List<User> users, Slice page) {
        if (users != null && users.size() > 0) {
            this.members = users.stream()
                    .map(user -> userInfo.toDto(user))
                    .collect(Collectors.toList());
        }
        this.pageInfo = PaginationDto.of(page);
    }
}
