package boogi.apiserver.domain.community.community.dao;

import boogi.apiserver.domain.community.community.domain.Community;
import boogi.apiserver.domain.community.community.domain.CommunityCategory;
import boogi.apiserver.domain.community.community.domain.QCommunity;
import boogi.apiserver.domain.community.community.dto.enums.CommunityListingOrder;
import boogi.apiserver.domain.community.community.dto.request.CommunityQueryRequest;
import boogi.apiserver.domain.community.community.dto.dto.SearchCommunityDto;
import boogi.apiserver.domain.hashtag.community.domain.QCommunityHashtag;
import boogi.apiserver.global.util.PageableUtil;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;


@RequiredArgsConstructor
public class CommunityRepositoryImpl implements CommunityRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    private final QCommunity community = QCommunity.community;
    private final QCommunityHashtag communityHashtag = QCommunityHashtag.communityHashtag;

    @Override
    public Slice<SearchCommunityDto> getSearchedCommunities(Pageable pageable, CommunityQueryRequest condition) {
        OrderSpecifier order = getOrderSpecifier(condition.getOrder());

        String keyword = condition.getKeyword();

        if (keyword == null) {
            Predicate[] where = {
                    privateEq(condition.getIsPrivate()),
                    categoryEq(condition.getCategory())
            };

            List<Community> communities = queryFactory.selectFrom(community)
                    .where(where)
                    .orderBy(order)
                    .offset(pageable.getOffset())
                    .limit(pageable.getPageSize() + 1)
                    .fetch();

            // CommunityHashtag LAZY INIT
            communities.stream().anyMatch(c -> c.getHashtags().getValues().size() != 0);

            List<SearchCommunityDto> dtos = transformToSearchCommunityDto(communities);

            return PageableUtil.getSlice(dtos, pageable);
        }

        QCommunity _community = new QCommunity("communitySub");
        Predicate[] where = {
                privateEq(condition.getIsPrivate()),
                categoryEq(condition.getCategory()),

                community.communityName.value.contains(keyword).or(
                        community.id.in(JPAExpressions.select(communityHashtag.community.id)
                                .from(communityHashtag)
                                .where(communityHashtag.tag.eq(keyword)))
                )
        };

        List<Community> communities = queryFactory.selectFrom(community)
                .where(where)
                .orderBy(order)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();
        //LAZY INIT
        communities.stream().anyMatch(c -> c.getHashtags().getValues().size() != 0);

        List<SearchCommunityDto> dtos = transformToSearchCommunityDto(communities);

        return PageableUtil.getSlice(dtos, pageable);
    }

    private OrderSpecifier getOrderSpecifier(CommunityListingOrder condition) {
        OrderSpecifier order = community.createdAt.desc();
        switch (condition) {
            case NEWER:
                order = community.createdAt.desc();
                break;
            case OLDER:
                order = community.createdAt.asc();
                break;
            case MANY_PEOPLE:
                order = community.memberCount.desc();
            case LESS_PEOPLE:
                order = community.memberCount.asc();
            default:
                order = community.createdAt.desc();

        }
        return order;
    }

    private BooleanExpression privateEq(Boolean isPrivate) {
        if (Objects.isNull(isPrivate)) {
            return null;
        }
        return isPrivate ? community.isPrivate.eq(true) : community.isPrivate.ne(true);
    }

    private BooleanExpression categoryEq(CommunityCategory category) {
        return Objects.isNull(category) ? null : community.category.eq(category);
    }

    private BooleanExpression communityNameContains(String keyword) {
        return Objects.isNull(keyword) ? null : community.communityName.value.contains(keyword);
    }

    private List<SearchCommunityDto> transformToSearchCommunityDto(List<Community> communities) {
        return communities.stream()
                .map(SearchCommunityDto::of)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Community> findCommunityById(Long communityId) {
        Community findCommunity = queryFactory.selectFrom(QCommunity.community)
                .where(
                        QCommunity.community.id.eq(communityId)
                ).fetchOne();

        return Optional.ofNullable(findCommunity);
    }
}
