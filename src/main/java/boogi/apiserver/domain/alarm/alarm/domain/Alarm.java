package boogi.apiserver.domain.alarm.alarm.domain;

import boogi.apiserver.domain.model.TimeBaseEntity;
import boogi.apiserver.domain.user.domain.User;
import lombok.*;

import javax.persistence.*;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Alarm extends TimeBaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alarm_id")
    private Long id;

    @JoinColumn(name = "user_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private String head;
    private String body;

    @Builder
    private Alarm(Long id, User user, String head, String body) {
        this.id = id;
        this.user = user;
        this.head = head;
        this.body = body;
    }

    public boolean isSameUser(Long userId) {
        return user.getId().equals(userId);
    }
}
