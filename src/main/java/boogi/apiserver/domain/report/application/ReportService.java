package boogi.apiserver.domain.report.application;


import boogi.apiserver.domain.comment.dao.CommentRepository;
import boogi.apiserver.domain.comment.domain.Comment;
import boogi.apiserver.domain.community.community.application.CommunityValidationService;
import boogi.apiserver.domain.community.community.dao.CommunityRepository;
import boogi.apiserver.domain.community.community.domain.Community;
import boogi.apiserver.domain.member.application.MemberValidationService;
import boogi.apiserver.domain.post.post.dao.PostRepository;
import boogi.apiserver.domain.post.post.domain.Post;
import boogi.apiserver.domain.report.dao.ReportRepository;
import boogi.apiserver.domain.report.domain.Report;
import boogi.apiserver.domain.report.domain.ReportTarget;
import boogi.apiserver.domain.report.dto.CreateReport;
import boogi.apiserver.domain.user.application.UserQueryService;
import boogi.apiserver.domain.user.domain.User;
import boogi.apiserver.global.error.exception.EntityNotFoundException;
import boogi.apiserver.global.error.exception.ErrorInfo;
import boogi.apiserver.global.error.exception.InvalidValueException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReportService {

    private final UserQueryService userQueryService;
    private final CommunityRepository communityRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ReportRepository reportRepository;

    private final CommunityValidationService communityValidationService;
    private final MemberValidationService memberValidationService;

    @Transactional
    public void createReport(CreateReport createReport, Long userId) {
        ReportTarget target = createReport.getTarget();
        Long id = createReport.getId();

        Long communityId;
        Report newReport;
        User reportUser = userQueryService.getUser(userId);

        switch (target) {
            case COMMUNITY:
                Community findCommunity = communityRepository.findCommunityById(id).orElseThrow(() -> {
                    throw new EntityNotFoundException("해당 신고 대상이 존재하지 않습니다", ErrorInfo.NOT_FOUND);
                });
                newReport = Report.of(
                        findCommunity,
                        reportUser,
                        createReport.getContent(),
                        createReport.getReason());
                break;
            case POST:
                Post findPost = postRepository.findPostById(id).orElseThrow(() -> {
                    throw new EntityNotFoundException("해당 신고 대상이 존재하지 않습니다", ErrorInfo.NOT_FOUND);
                });

                communityId = findPost.getCommunity().getId();
                if (communityValidationService.checkOnlyPrivateCommunity(communityId)) {
                    memberValidationService.checkMemberJoinedCommunity(userId, communityId);
                }
                newReport = Report.of(
                        findPost,
                        reportUser,
                        createReport.getContent(),
                        createReport.getReason());
                break;
            case COMMENT:
                Comment findComment = commentRepository.findCommentById(id).orElseThrow(() -> {
                    throw new EntityNotFoundException("해당 신고 대상이 존재하지 않습니다", ErrorInfo.NOT_FOUND);
                });

                communityId = findComment.getPost().getCommunity().getId();
                if (communityValidationService.checkOnlyPrivateCommunity(communityId)) {
                    memberValidationService.checkMemberJoinedCommunity(userId, communityId);
                }
                newReport = Report.of(
                        findComment,
                        reportUser,
                        createReport.getContent(),
                        createReport.getReason());
                break;
            default:
                throw new InvalidValueException("잘못된 신고 대상입니다");
        }

        reportRepository.save(newReport);
    }
}