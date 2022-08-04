package boogi.apiserver.domain.alarm.alarmconfig.dao;

import boogi.apiserver.domain.alarm.alarmconfig.domain.AlarmConfig;
import boogi.apiserver.domain.alarm.alarmconfig.domain.QAlarmConfig;
import com.querydsl.jpa.impl.JPAQueryFactory;

import javax.persistence.EntityManager;
import java.util.List;

public class AlarmConfigRepositoryCustomImpl implements AlarmConfigRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QAlarmConfig alarmConfig = QAlarmConfig.alarmConfig;

    public AlarmConfigRepositoryCustomImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public AlarmConfig getAlarmConfigByUserId(Long userId) {
        List<AlarmConfig> configs = queryFactory.selectFrom(alarmConfig)
                .where(alarmConfig.user.id.eq(userId))
                .orderBy(alarmConfig.createdAt.desc())
                .fetch();

        return configs.size() >= 1 ? configs.get(0) : null;
    }
}
