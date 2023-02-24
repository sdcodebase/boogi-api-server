package boogi.apiserver.domain.community.community.domain;

import boogi.apiserver.domain.hashtag.community.domain.CommunityHashtag;
import boogi.apiserver.domain.hashtag.community.domain.CommunityHashtags;
import boogi.apiserver.domain.model.TimeBaseEntity;
import lombok.*;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "COMMUNITY")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Where(clause = "deleted_at is null")
public class Community extends TimeBaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "community_id")
    private Long id;

    @Embedded
    private CommunityName communityName;

    @Embedded
    private Description description;

    @Column(name = "private")
    private boolean isPrivate;

    @Enumerated(value = EnumType.STRING)
    private CommunityCategory category;

    @Column(name = "member_count")
    private int memberCount;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "auto_approval")
    private boolean autoApproval;

    @Embedded
    private CommunityHashtags hashtags = new CommunityHashtags();

    @Builder
    private Community(final Long id, final String communityName, final String description, final boolean isPrivate,
                      final CommunityCategory category, final int memberCount, final LocalDateTime deletedAt,
                      final boolean autoApproval, final List<CommunityHashtag> hashtags) {
        this.id = id;
        this.communityName = new CommunityName(communityName);
        this.description = new Description(description);
        this.isPrivate = isPrivate;
        this.category = category;
        this.memberCount = memberCount;
        this.deletedAt = deletedAt;
        this.autoApproval = autoApproval;
        this.hashtags = new CommunityHashtags(hashtags);
    }

    private Community(String name, String description, boolean isPrivate, boolean autoApproval, CommunityCategory category) {
        this.communityName = new CommunityName(name);
        this.description = new Description(description);
        this.isPrivate = isPrivate;
        this.autoApproval = autoApproval;
        this.category = category;
    }

    public static Community of(String name, String description, boolean isPrivate, boolean autoApproval, CommunityCategory category) {
        return new Community(name, description, isPrivate, autoApproval, category);
    }

    public void updateDescription(String description) {
        this.description = new Description(description);
    }

    public void addTags(List<String> tags) {
        this.hashtags.addTags(tags, this);
    }

    public void updateCommunity(String description, List<String> tags) {
        this.description = new Description(description);
        this.hashtags.updateTags(tags, this);
    }

    public void toPublic() {
        this.isPrivate = false;
    }

    public void toPrivate() {
        this.isPrivate = true;
    }

    public void openAutoApproval() {
        this.autoApproval = true;
    }

    public void closeAutoApproval() {
        this.autoApproval = false;
    }

    public void shutdown() {
        this.deletedAt = LocalDateTime.now();
    }

    public void addMemberCount() {
        this.memberCount++;
    }

    public Long getId() {
        return id;
    }

    public String getCommunityName() {
        return communityName.getValue();
    }

    public String getDescription() {
        return description.getValue();
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public CommunityCategory getCategory() {
        return category;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public boolean isAutoApproval() {
        return autoApproval;
    }

    public CommunityHashtags getHashtags() {
        return hashtags;
    }
}
