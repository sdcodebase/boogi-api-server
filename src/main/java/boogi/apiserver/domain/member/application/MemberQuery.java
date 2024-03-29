package boogi.apiserver.domain.member.application;

import boogi.apiserver.domain.community.community.repository.CommunityRepository;
import boogi.apiserver.domain.community.community.domain.Community;
import boogi.apiserver.domain.member.repository.MemberRepository;
import boogi.apiserver.domain.member.domain.Member;
import boogi.apiserver.domain.member.dto.dto.BannedMemberDto;
import boogi.apiserver.domain.member.dto.dto.MemberDto;
import boogi.apiserver.domain.member.dto.response.JoinedMembersPageResponse;
import boogi.apiserver.domain.member.exception.NotJoinedMemberException;
import boogi.apiserver.domain.member.exception.NotViewableMemberException;
import boogi.apiserver.domain.member.vo.NullMember;
import boogi.apiserver.domain.user.dto.dto.UserBasicProfileDto;
import boogi.apiserver.domain.user.dto.dto.UserJoinedCommunityDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberQuery {
    private final MemberRepository memberRepository;
    private final CommunityRepository communityRepository;

    public Member getMember(Long userId, Long communityId) {
        return memberRepository.findByUserIdAndCommunityId(userId, communityId)
                .orElseThrow(NotJoinedMemberException::new);
    }

    public Member getManager(Long userId, Long communityId) {
        Member member = this.getMember(userId, communityId);
        member.validateManager();
        return member;
    }

    public Member getOperator(Long userId, Long communityId) {
        Member member = this.getMember(userId, communityId);
        member.validateOperator();
        return member;
    }

    public Member getMemberOrNullMember(Long userId, Community community) {
        return memberRepository.findByUserIdAndCommunityId(userId, community.getId())
                .orElse(new NullMember());
    }

    public Member getViewableMember(Long userId, Community community) {
        Member member = getMemberOrNullMember(userId, community);
        if (!community.canViewMember(member)) {
            throw new NotViewableMemberException();
        }
        return member;
    }

    public List<UserJoinedCommunityDto> getJoinedMemberInfo(Long userId) {
        return memberRepository.findByUserId(userId).stream()
                .map(Member::getCommunity)
                .map(UserJoinedCommunityDto::from)
                .collect(Collectors.toList());
    }

    public JoinedMembersPageResponse getCommunityJoinedMembers(Pageable pageable, Long communityId) {
        communityRepository.findCommunityById(communityId);
        final Slice<Member> members = memberRepository.findJoinedMembers(pageable, communityId);
        return JoinedMembersPageResponse.from(members);
    }

    public List<BannedMemberDto> getBannedMembers(Long userId, Long communityId) {
        communityRepository.findCommunityById(communityId);
        this.getOperator(userId, communityId);
        return memberRepository.findBannedMembers(communityId);
    }

    public Slice<UserBasicProfileDto> getMentionSearchMembers(Pageable pageable, Long communityId, String name) {
        return memberRepository.findMentionMember(pageable, communityId, name);
    }

    public List<MemberDto> getJoinedMembersAll(Long communityId, Long userId) {
        communityRepository.findCommunityById(communityId);
        Member sessionMember = getMember(userId, communityId);

        List<Member> findAllJoinedMembers = memberRepository.findAllJoinedMembersWithUser(communityId);
        findAllJoinedMembers.remove(sessionMember);

        return MemberDto.listOf(findAllJoinedMembers);
    }
}
