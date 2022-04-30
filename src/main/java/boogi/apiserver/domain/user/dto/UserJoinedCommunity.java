package boogi.apiserver.domain.user.dto;

import boogi.apiserver.domain.community.community.domain.Community;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@AllArgsConstructor
@Data
public class UserJoinedCommunity {
    private String name;
    private Long id;

    public static UserJoinedCommunity of(Community community) {
        return UserJoinedCommunity.builder()
                .id(community.getId())
                .name(community.getCommunityName())
                .build();
    }
}