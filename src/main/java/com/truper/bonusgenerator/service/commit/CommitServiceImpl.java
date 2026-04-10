package com.truper.bonusgenerator.service.commit;

import com.truper.bonusgenerator.model.dto.CommitDto;
import com.truper.bonusgenerator.model.dto.mapper.CommitMapper;
import com.truper.bonusgenerator.model.entity.Commit;
import com.truper.bonusgenerator.repository.CommitRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.hibernate.grammars.hql.HqlParser;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommitServiceImpl implements CommitService {

    private final CommitRepository commitRepository;
    private final CommitMapper commitMapper;

    @Override
    @Transactional()
    public CommitDto createCommit(CommitDto commit) {
        Commit commitEntity = commitMapper.toEntity(commit);
        return commitMapper.toDto(commitRepository.save(commitEntity));
    }

    @Override
    public List<CommitDto> getCommitsByRangeDate() {
//        List<Commit> commits = commitRepository.findByCreatedAtBetween(
//                getReportRangeForLastDayOfMonth().get("start").atStartOfDay(),
//                getReportRangeForLastDayOfMonth().get("end").atTime(23, 59, 59)
//        );

//        return commits.stream()
//                .map(commitMapper::toDto)
//                .toList();
        return null;
    }

    public Map<String, LocalDate> getReportRangeForLastDayOfMonth() {
        LocalDate lastDay = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());
        Map<String, LocalDate> range = Map.of();
        DayOfWeek day = getLastDayOfMonth();

        switch (day) {
            case MONDAY:
                range = calculateRange(lastDay, lastDay.plusDays(4));
                break;
            case TUESDAY:
                range = calculateRange(lastDay.minusDays(1), lastDay.plusDays(3));
                break;
            case WEDNESDAY:
                range = calculateRange(lastDay.minusDays(2), lastDay.plusDays(2));
                break;
            case THURSDAY:
                range = calculateRange(lastDay.minusDays(3), lastDay.plusDays(3));
                break;
            case FRIDAY:
                range = calculateRange(lastDay.minusDays(4), lastDay.plusDays(2));
                break;
            case SATURDAY:
                range = calculateRange(lastDay.minusDays(5), lastDay.plusDays(1));
                break;
            case SUNDAY:
                range = calculateRange(lastDay.minusDays(6), lastDay);
                break;
        }

        return range;

    }

    private static @NonNull Map<String, LocalDate> calculateRange(LocalDate lastMonday, LocalDate nextFriday) {
        Map<String, LocalDate> range;
        range = Map.of(
                "start", lastMonday,
                "end", nextFriday
        );
        return range;
    }

    private DayOfWeek getLastDayOfMonth() {
        LocalDate now = LocalDate.now();
        LocalDate lastDay = now.with(TemporalAdjusters.lastDayOfMonth());

        return lastDay.getDayOfWeek();
    }


}
