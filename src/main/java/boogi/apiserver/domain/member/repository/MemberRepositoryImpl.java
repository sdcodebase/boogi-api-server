package boogi.apiserver.domain.member.repository;

import boogi.apiserver.domain.member.domain.Member;
import boogi.apiserver.domain.member.domain.MemberType;
import boogi.apiserver.domain.member.domain.QMember;
import boogi.apiserver.domain.member.dto.dto.BannedMemberDto;
import boogi.apiserver.domain.member.dto.dto.QBannedMemberDto;
import boogi.apiserver.domain.user.dto.dto.QUserBasicProfileDto;
import boogi.apiserver.domain.user.dto.dto.UserBasicProfileDto;
import boogi.apiserver.global.util.PageableUtil;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static boogi.apiserver.domain.community.community.domain.QCommunity.community;
import static boogi.apiserver.domain.member.domain.QMember.member;
import static boogi.apiserver.domain.user.domain.QUser.user;


@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<Member> findByUserId(Long userId) {
        return queryFactory.select(member)
                .from(member)
                .where(member.user.id.eq(userId),
                        member.bannedAt.isNull()
                )
                .fetch();
    }

    @Override
    public List<Member> findMembersWithCommunity(Long userId) {
        return queryFactory.selectFrom(member)
                .where(member.user.id.eq(userId),
                        member.bannedAt.isNull()
                )
                .join(member.community).fetchJoin()
                .orderBy(member.createdAt.desc())
                .fetch();
    }

    @Override
    public Optional<Member> findByUserIdAndCommunityId(Long userId, Long communityId) {
        Member findMember = queryFactory.selectFrom(member)
                .where(
                        member.user.id.eq(userId),
                        member.community.id.eq(communityId),
                        member.bannedAt.isNull()
                ).orderBy(member.createdAt.desc())
                .limit(1)
                .fetchOne();

        return Optional.ofNullable(findMember);
    }

    @Override
    public Slice<Member> findJoinedMembers(Pageable pageable, Long communityId) {
        NumberExpression<Integer> caseBuilder = new CaseBuilder()
                .when(member.memberType.eq(MemberType.MANAGER)).then(3)
                .when(member.memberType.eq(MemberType.SUB_MANAGER)).then(2)
                .when(member.memberType.eq(MemberType.NORMAL)).then(1)
                .otherwise(0);

        List<Member> members = queryFactory.select(member)
                .from(member)
                .where(
                        member.community.id.eq(communityId),
                        member.bannedAt.isNull()
                )
                .innerJoin(member.user, user).fetchJoin()
                .orderBy(
                        caseBuilder.desc(),
                        member.createdAt.asc()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        return PageableUtil.getSlice(members, pageable);
    }

    @Override
    public List<Member> findAllJoinedMembersWithUser(Long communityId) {
        return queryFactory.selectFrom(member)
                .where(
                        member.community.id.eq(communityId),
                        member.bannedAt.isNull()
                )
                .join(member.user, user).fetchJoin()
                .fetch();
    }

    @Override
    public Optional<Member> findAnyMemberExceptManager(Long communityId) {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.community.id.eq(communityId),
                        member.memberType.ne(MemberType.MANAGER),
                        member.bannedAt.isNull()
                ).limit(1)
                .fetchOne();
        return Optional.ofNullable(findMember);
    }

    @Override
    public List<BannedMemberDto> findBannedMembers(Long communityId) {
        return queryFactory
                .select(new QBannedMemberDto(member.id, member.user))
                .from(member)
                .where(
                        member.community.id.eq(communityId),
                        member.bannedAt.isNotNull()
                )
                .join(member.user, user)
                .orderBy(member.bannedAt.desc())
                .fetch();
    }

    @Override
    public List<Member> findAlreadyJoinedMember(List<Long> userIds, Long communityId) {
        return queryFactory
                .selectFrom(member)
                .where(member.community.id.eq(communityId),
                        member.user.id.in(userIds)
                ).fetch();
    }

    @Override
    public List<Long> findMemberIdsForQueryUserPost(Long userId, Long sessionUserId) {
        QMember memberSub = new QMember("memberSub");

        JPQLQuery<Long> sameJoinedCommunityIdsQuery = JPAExpressions
                .select(memberSub.community.id)
                .from(memberSub)
                .where(
                        memberSub.user.id.in(userId, sessionUserId),
                        memberSub.bannedAt.isNull()
                )
                .groupBy(memberSub.community.id)
                .having(memberSub.community.id.count().lt(2));

        JPQLQuery<Long> privateCommunityFromSameJoinedCommunityQuery = JPAExpressions
                .select(community.id)
                .from(community)
                .where(
                        community.isPrivate.isTrue(),
                        community.id.in(
                                sameJoinedCommunityIdsQuery
                        )
                );

        return queryFactory.select(member.id)
                .from(member)
                .where(
                        member.user.id.eq(userId),
                        member.bannedAt.isNull(),
                        member.community.id.notIn(
                                privateCommunityFromSameJoinedCommunityQuery
                        )
                ).fetch();
    }

    @Override
    public List<Long> findMemberIdsForQueryUserPost(Long sessionUserId) {
        return queryFactory.select(member.id)
                .from(member)
                .where(
                        member.user.id.eq(sessionUserId),
                        member.bannedAt.isNull()
                ).fetch();
    }

    @Override
    public Member findManager(Long communityId) {
        return queryFactory.selectFrom(member)
                .where(
                        member.community.id.eq(communityId),
                        member.memberType.eq(MemberType.MANAGER),
                        member.bannedAt.isNull()
                )
                .fetchFirst();
    }

    @Override
    public Slice<UserBasicProfileDto> findMentionMember(Pageable pageable, Long communityId, String name) {
        List<UserBasicProfileDto> members =
                queryFactory.select(new QUserBasicProfileDto(member.user))
                        .from(member)
                        .where(
                                member.community.id.eq(communityId),
                                member.bannedAt.isNull(),
                                nameContains(name) //todo: username index 추가
                        ).join(member.user)
                        .orderBy(member.user.username.value.asc())
                        .offset(pageable.getOffset())
                        .limit(pageable.getPageSize() + 1)
                        .fetch();

        return PageableUtil.getSlice(members, pageable);
    }

    private BooleanExpression nameContains(String name) {
        return Objects.isNull(name) ? null : member.user.username.value.contains(name);
    }
}
