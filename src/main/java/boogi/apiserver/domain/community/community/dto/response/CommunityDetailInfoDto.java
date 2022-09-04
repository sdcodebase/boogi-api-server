package boogi.apiserver.domain.community.community.dto.response;

import boogi.apiserver.domain.community.community.domain.Community;
import boogi.apiserver.domain.hashtag.community.domain.CommunityHashtag;
import boogi.apiserver.global.util.time.TimePattern;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@AllArgsConstructor
public class CommunityDetailInfoDto {
    private Boolean isPrivated;
    private String category;
    private String name;
    private String introduce;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> hashtags;
    private String memberCount;

    @JsonFormat(pattern = TimePattern.BASIC_FORMAT_STRING)
    private LocalDateTime createdAt;

    private CommunityDetailInfoDto(Community community, List<CommunityHashtag> hashtags) {
        this.isPrivated = community.isPrivate();
        this.name = community.getCommunityName();
        this.introduce = community.getDescription();

        if (hashtags != null && hashtags.size() > 0) {
            this.hashtags = hashtags.stream()
                    .map(CommunityHashtag::getTag)
                    .collect(Collectors.toList());
        }
        this.memberCount = String.valueOf(community.getMemberCount());
        this.createdAt = community.getCreatedAt();
        this.category = community.getCategory().toString();
    }

    public static CommunityDetailInfoDto of(Community community) {
        return new CommunityDetailInfoDto(community, community.getHashtags());
    }
}