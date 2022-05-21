package boogi.apiserver.domain.member.dao;

import boogi.apiserver.domain.member.domain.Member;
import boogi.apiserver.domain.member.dto.BannedMemberDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MemberRepositoryCustom {

    List<Member> findByUserId(Long userId);

    List<Member> findWhatIJoined(Long userId);

    List<Member> findByUserIdAndCommunityId(Long userId, Long communityId);

    Page<Member> findJoinedMembers(Pageable pageable, Long communityId);

    List<Member> findJoinedMembersAllWithUserByCommunityId(Long communityId);

    Member findAnyMemberExceptManager(Long communityId);

    List<BannedMemberDto> findBannedMembers(Long communityId);

    List<Member> findAlreadyJoinedMemberByUserId(List<Long> userIds, Long communityId);
}
