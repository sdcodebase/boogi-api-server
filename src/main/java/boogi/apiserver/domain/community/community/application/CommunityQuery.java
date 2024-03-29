package boogi.apiserver.domain.community.community.application;

import boogi.apiserver.domain.community.community.repository.CommunityRepository;
import boogi.apiserver.domain.community.community.domain.Community;
import boogi.apiserver.domain.community.community.dto.dto.CommunityMetadataDto;
import boogi.apiserver.domain.community.community.dto.dto.CommunitySettingInfoDto;
import boogi.apiserver.domain.community.community.dto.dto.JoinedCommunitiesDto;
import boogi.apiserver.domain.community.community.dto.dto.SearchCommunityDto;
import boogi.apiserver.domain.community.community.dto.request.CommunityQueryRequest;
import boogi.apiserver.domain.community.community.dto.response.CommunityDetailResponse;
import boogi.apiserver.domain.member.application.MemberQuery;
import boogi.apiserver.domain.member.repository.MemberRepository;
import boogi.apiserver.domain.member.domain.Member;
import boogi.apiserver.domain.notice.application.NoticeQuery;
import boogi.apiserver.domain.notice.dto.dto.NoticeDto;
import boogi.apiserver.domain.post.post.application.PostQuery;
import boogi.apiserver.domain.post.post.repository.PostRepository;
import boogi.apiserver.domain.post.post.domain.Post;
import boogi.apiserver.domain.post.post.dto.dto.LatestCommunityPostDto;
import boogi.apiserver.domain.post.postmedia.repository.PostMediaRepository;
import boogi.apiserver.domain.post.postmedia.domain.PostMedia;
import boogi.apiserver.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CommunityQuery {
    private final CommunityRepository communityRepository;
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final PostMediaRepository postMediaRepository;

    private final MemberQuery memberQuery;
    private final NoticeQuery noticeQuery;
    private final PostQuery postQuery;

    public CommunityDetailResponse getCommunityDetail(Long userId, Long communityId) {
        Community community = communityRepository.findCommunityById(communityId);
        Member member = memberQuery.getMemberOrNullMember(userId, community);

        List<NoticeDto> communityNotices = noticeQuery.getCommunityLatestNotice(communityId);
        List<LatestCommunityPostDto> latestPosts = postQuery.getLatestPostOfCommunity(member, community);

        return CommunityDetailResponse.of(communityNotices, latestPosts, member, community);
    }

    public CommunityMetadataDto getCommunityMetadata(Long userId, Long communityId) {
        final Community community = communityRepository.findCommunityById(communityId);
        memberQuery.getManager(userId, communityId);

        return CommunityMetadataDto.of(community);
    }

    public Slice<SearchCommunityDto> getSearchedCommunities(Pageable pageable, CommunityQueryRequest request) {
        return communityRepository.getSearchedCommunities(pageable, request);
    }

    public CommunitySettingInfoDto getSetting(Long userId, Long communityId) {
        Community community = communityRepository.findCommunityById(communityId);
        memberQuery.getManager(userId, communityId);

        return CommunitySettingInfoDto.of(community);
    }

    public JoinedCommunitiesDto getJoinedCommunitiesWithLatestPost(Long userId) {
        userRepository.findUserById(userId);

        Map<Long, Community> joinedCommunityMap = getJoinedCommunityMap(userId);

        List<Post> latestPosts = postRepository.getLatestPostByCommunityIds(joinedCommunityMap.keySet());
        List<Long> latestPostIds = latestPosts.stream()
                .map(Post::getId)
                .collect(Collectors.toList());

        List<PostMedia> postMedias = postMediaRepository.getPostMediasByLatestPostIds(latestPostIds);

        return JoinedCommunitiesDto.of(joinedCommunityMap, latestPosts, postMedias);
    }

    private LinkedHashMap<Long, Community> getJoinedCommunityMap(Long userId) {
        List<Member> findMembers = memberRepository.findMembersWithCommunity(userId);
        return findMembers.stream()
                .map(Member::getCommunity)
                .collect(Collectors.toMap(
                        m1 -> m1.getId(),
                        m2 -> m2,
                        (o, n) -> n,
                        LinkedHashMap::new
                ));
    }
}
