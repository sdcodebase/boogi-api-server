package boogi.apiserver.domain.comment.application;

import boogi.apiserver.domain.comment.dao.CommentRepository;
import boogi.apiserver.domain.comment.domain.Comment;
import boogi.apiserver.domain.comment.dto.request.CreateCommentRequest;
import boogi.apiserver.domain.comment.dto.response.CommentsAtPostResponse;
import boogi.apiserver.domain.community.community.domain.Community;
import boogi.apiserver.domain.comment.dto.response.UserCommentPageResponse;
import boogi.apiserver.domain.like.application.LikeCommandService;
import boogi.apiserver.domain.like.dao.LikeRepository;
import boogi.apiserver.domain.like.domain.Like;
import boogi.apiserver.domain.member.application.MemberValidationService;
import boogi.apiserver.domain.member.dao.MemberRepository;
import boogi.apiserver.domain.member.domain.Member;
import boogi.apiserver.domain.member.domain.MemberType;
import boogi.apiserver.domain.member.exception.NotAuthorizedMemberException;
import boogi.apiserver.domain.member.exception.NotJoinedMemberException;
import boogi.apiserver.domain.post.post.application.PostQueryService;
import boogi.apiserver.domain.post.post.dao.PostRepository;
import boogi.apiserver.domain.post.post.domain.Post;
import boogi.apiserver.domain.user.dao.UserRepository;
import boogi.apiserver.global.error.exception.EntityNotFoundException;
import boogi.apiserver.global.webclient.push.MentionType;
import boogi.apiserver.global.webclient.push.SendPushNotification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@Transactional
@RequiredArgsConstructor
public class CommentCommandService {

    private final PostQueryService postQueryService;

    private final UserRepository userRepository;

    private final MemberRepository memberRepository;
    private final MemberValidationService memberValidationService;

    private final LikeRepository likeRepository;
    private final LikeCommandService likeCommandService;

    private final CommentRepository commentRepository;
    private final CommentValidationService commentValidationService;

    private final SendPushNotification sendPushNotification;
    private final PostRepository postRepository;

    public Comment createComment(CreateCommentRequest createCommentRequest, Long userId) {
        Post findPost = postRepository.findByPostId(createCommentRequest.getPostId());

        Member member = memberRepository.findByUserIdAndCommunityId(userId, findPost.getCommunity().getId())
                .orElseThrow(NotJoinedMemberException::new);

        Long parentCommentId = createCommentRequest.getParentCommentId();
        commentValidationService.checkCommentMaxDepthOver(parentCommentId);

        Comment findParentComment = parentCommentId == null ? null : commentRepository.findById(parentCommentId)
                .orElse(null);

        Comment newComment = Comment.of(findPost, member, findParentComment, createCommentRequest.getContent());
        findPost.addCommentCount();

        commentRepository.save(newComment);
        Long savedCommentId = newComment.getId();

        sendPushNotification.commentNotification(savedCommentId);
        if (createCommentRequest.getMentionedUserIds().isEmpty() == false) {
            sendPushNotification.mentionNotification(createCommentRequest.getMentionedUserIds(), savedCommentId, MentionType.COMMENT);
        }

        return newComment;
    }

    public void deleteComment(Long commentId, Long userId) {
        Comment findComment = commentRepository.findCommentWithMemberByCommentId(commentId)
                .orElseThrow(() -> new EntityNotFoundException("해당 댓글이 존재하지 않습니다."));

        Long joinedCommunityId = findComment.getMember().getCommunity().getId();
        Long commentedUserId = findComment.getMember().getUser().getId();
        if (commentedUserId.equals(userId) ||
                memberValidationService.hasAuth(userId, joinedCommunityId, MemberType.SUB_MANAGER)) {
            likeCommandService.removeAllCommentLikes(findComment.getId());

            findComment.deleteComment();
        } else {
            throw new NotAuthorizedMemberException("해당 댓글의 삭제 권한이 없습니다.");
        }
    }

    public CommentsAtPostResponse getCommentsAtPost(Long postId, Long userId, Pageable pageable) {
        Post findPost = postRepository.findByPostId(postId);

        Community postedCommunity = findPost.getCommunity();
        Member member = memberRepository.findByUserIdAndCommunityId(userId, postedCommunity.getId())
                .orElse(null);

        if (postedCommunity.isPrivate() && member == null) {
            throw new NotJoinedMemberException();
        }

        Slice<Comment> commentPage = commentRepository.findParentCommentsWithMemberByPostId(pageable, postId);

        List<Comment> parentComments = commentPage.getContent().stream()
                .map(c -> {
                    if (c.getDeletedAt() != null) {
                        return Comment.deletedOf(c.getId(), c.getDeletedAt());
                    }
                    return c;
                })
                .collect(Collectors.toList());

        List<Long> parentCommentIds = parentComments.stream()
                .map(Comment::getId)
                .collect(Collectors.toList());

        List<Comment> childComments = commentRepository.findChildCommentsWithMemberByParentCommentIds(parentCommentIds);

        List<Long> commentIds = childComments.stream()
                .map(Comment::getId)
                .collect(Collectors.toList());
        commentIds.addAll(parentCommentIds);

        Map<Long, Long> findCommentCountMap = (commentIds.isEmpty()) ? new HashMap<>() :
                likeRepository.getCommentLikeCountsByCommentIds(commentIds);

        Long joinedMemberId = (member == null) ? null : member.getId();
        Map<Long, Like> commentLikes = (joinedMemberId == null) ? null :
                likeRepository.findCommentLikesByCommentIdsAndMemberId(commentIds, joinedMemberId).stream()
                        .collect(Collectors.toMap(c -> c.getComment().getId(), c -> c));

        Map<Long, List<CommentsAtPostResponse.ChildCommentInfo>> childCommentInfos = childComments.stream()
                .map(c -> createChildCommentInfo(
                        joinedMemberId,
                        commentLikes,
                        c,
                        findCommentCountMap.getOrDefault(c.getId(), 0L)))
                .collect(Collectors.groupingBy(CommentsAtPostResponse.ChildCommentInfo::getParentId, HashMap::new, Collectors.toCollection(ArrayList::new)));

        List<CommentsAtPostResponse.ParentCommentInfo> commentInfos = parentComments.stream()
                .filter(c -> (c.getDeletedAt() == null || childCommentInfos.get(c.getId()) != null))
                .map(c -> createParentCommentInfo(joinedMemberId,
                        commentLikes,
                        childCommentInfos,
                        c,
                        findCommentCountMap.getOrDefault(c.getId(), 0L)))
                .collect(Collectors.toList());

        return CommentsAtPostResponse.of(commentInfos, commentPage);
    }

    private CommentsAtPostResponse.ChildCommentInfo createChildCommentInfo(Long joinedMemberId, Map<Long, Like> commentLikes, Comment c, Long likeCount) {
        Like like = (commentLikes == null) ? null : commentLikes.get(c.getId());
        Long likeId = (like == null) ? null : like.getId();
        Member commentedMember = c.getMember();
        Long commentedMemberId = (commentedMember == null) ? null : commentedMember.getId();
        Boolean me = (commentedMember != null && commentedMemberId.equals(joinedMemberId))
                ? Boolean.TRUE : Boolean.FALSE;
        Long parentId = c.getParent().getId();
        return CommentsAtPostResponse.ChildCommentInfo.of(c, likeId, me, parentId, likeCount);
    }

    private CommentsAtPostResponse.ParentCommentInfo createParentCommentInfo(Long joinedMemberId, Map<Long, Like> commentLikes, Map<Long, List<CommentsAtPostResponse.ChildCommentInfo>> childCommentInfos, Comment c, Long likeCount) {
        Like like = (commentLikes == null) ? null : commentLikes.get(c.getId());
        Long likeId = (like == null) ? null : like.getId();
        Member commentedMember = c.getMember();
        Long commentedMemberId = (commentedMember == null) ? null : commentedMember.getId();
        Boolean me = (commentedMember != null && commentedMemberId.equals(joinedMemberId))
                ? Boolean.TRUE : Boolean.FALSE;
        return CommentsAtPostResponse.ParentCommentInfo.of(c, likeId, me, childCommentInfos.get(c.getId()), likeCount);
    }

    public UserCommentPageResponse getUserComments(Long userId, Long sessionUserId, Pageable pageable) {
        List<Long> findMemberIds;

        if (userId.equals(sessionUserId)) {
            findMemberIds = memberRepository
                    .findMemberIdsForQueryUserPost(sessionUserId);
        } else {
            userRepository.findUserById(userId).orElseThrow(() ->
                    new EntityNotFoundException("해당 유저가 존재하지 않습니다."));

            findMemberIds = memberRepository
                    .findMemberIdsForQueryUserPost(userId, sessionUserId);
        }

        Slice<Comment> slice = commentRepository.getUserCommentPageByMemberIds(findMemberIds, pageable);

        return UserCommentPageResponse.of(slice);
    }
}
