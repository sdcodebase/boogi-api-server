package boogi.apiserver.domain.alarm.alarmconfig.domain;

import boogi.apiserver.domain.model.TimeBaseEntity;
import boogi.apiserver.domain.user.domain.User;
import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = "ALARM_CONFIG")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class AlarmConfig extends TimeBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alarm_config_id")
    private Long id;

    @JoinColumn(name = "user_id")
    @OneToOne(fetch = FetchType.LAZY)
    private User user;

    @Column(name = "message_alarm")
    @Setter
    private Boolean message;

    @Column(name = "notice_alarm")
    @Setter
    private Boolean notice;

    @Column(name = "join_request_alarm")
    @Setter
    private Boolean joinRequest;

    @Column(name = "comment_alarm")
    @Setter
    private Boolean comment;

    @Column(name = "mention_alarm")
    @Setter
    private Boolean mention;

    private AlarmConfig(User user) {
        this.user = user;
        this.message = true;
        this.notice = true;
        this.joinRequest = true;
        this.comment = true;
        this.mention = true;
    }

    public static AlarmConfig of(User user) {
        return new AlarmConfig(user);
    }
}
