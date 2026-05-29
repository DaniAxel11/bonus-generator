package com.truper.bonusgenerator.service.commit;

import com.truper.bonusgenerator.model.dto.CommitDto;
import com.truper.bonusgenerator.model.dto.mapper.CommitMapper;
import com.truper.bonusgenerator.model.dto.response.CommitMonthWeeksResponse;
import com.truper.bonusgenerator.model.dto.response.CommitWeekResponse;
import com.truper.bonusgenerator.model.entity.Commit;
import com.truper.bonusgenerator.repository.CommitRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommitServiceImpl implements CommitService {

    private final CommitRepository commitRepository;
    private final CommitMapper commitMapper;

    @Override
    @Transactional
    public CommitDto createCommit(CommitDto commit) {
        Commit commitEntity = commitMapper.toEntity(commit);
        return commitMapper.toDto(commitRepository.save(commitEntity));
    }

    @Override
    public List<CommitDto> getCommitsByRangeDate() {
        return null;
    }

    @Override
    public CommitMonthWeeksResponse getCurrentMonthCommitsByWeek() {
        WeekRange lastCompleteWeek = getLastCompleteWeek(LocalDate.now());
        YearMonth reportMonth = getReportMonth(lastCompleteWeek);

        List<CommitDto> weekCommits = getCommitsByDateRange(lastCompleteWeek.startDate(), lastCompleteWeek.endDate());

        CommitWeekResponse week = new CommitWeekResponse(
                getWeekNumber(reportMonth, lastCompleteWeek),
                lastCompleteWeek.startDate(),
                lastCompleteWeek.endDate(),
                weekCommits.size(),
                weekCommits
        );

        return new CommitMonthWeeksResponse(
                reportMonth.getYear(),
                reportMonth.getMonthValue(),
                1,
                List.of(week)
        );
    }

    @Override
    public List<CommitDto> getCommitsByDateRange(LocalDate startDate, LocalDate endDate) {
        Date start = toStartDate(startDate);
        Date end = toEndDate(endDate);
        return commitRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(start, end)
                .stream()
                .map(commitMapper::toDto)
                .toList();
    }

    private WeekRange getLastCompleteWeek(LocalDate currentDate) {
        LocalDate currentWeekStart = getStartOfWeek(currentDate);
        LocalDate lastCompleteWeekStart = currentWeekStart.minusWeeks(1);
        return new WeekRange(lastCompleteWeekStart, lastCompleteWeekStart.plusDays(6));
    }

    private int getWeekNumber(YearMonth month, WeekRange weekRange) {
        List<WeekRange> weekRanges = getWeeksAssignedToMonth(month);
        for (int i = 0; i < weekRanges.size(); i++) {
            if (weekRanges.get(i).startDate().equals(weekRange.startDate())) {
                return i + 1;
            }
        }
        return 1;
    }

    private YearMonth getReportMonth(WeekRange weekRange) {
        YearMonth startMonth = YearMonth.from(weekRange.startDate());
        YearMonth endMonth = YearMonth.from(weekRange.endDate());

        if (startMonth.equals(endMonth)) {
            return startMonth;
        }

        int startMonthDays = countDaysInMonth(weekRange.startDate(), weekRange.endDate(), startMonth);
        int endMonthDays = countDaysInMonth(weekRange.startDate(), weekRange.endDate(), endMonth);
        return startMonthDays >= endMonthDays ? startMonth : endMonth;
    }

    private List<WeekRange> getWeeksAssignedToMonth(YearMonth month) {
        LocalDate firstDayOfMonth = month.atDay(1);
        LocalDate lastDayOfMonth = month.atEndOfMonth();
        LocalDate currentWeekStart = getStartOfWeek(firstDayOfMonth);
        List<WeekRange> weekRanges = new ArrayList<>();

        while (!currentWeekStart.isAfter(lastDayOfMonth)) {
            LocalDate currentWeekEnd = currentWeekStart.plusDays(6);
            if (countDaysInMonth(currentWeekStart, currentWeekEnd, month) >= 4) {
                weekRanges.add(new WeekRange(currentWeekStart, currentWeekEnd));
            }
            currentWeekStart = currentWeekStart.plusWeeks(1);
        }

        return weekRanges;
    }

    private int countDaysInMonth(LocalDate startDate, LocalDate endDate, YearMonth month) {
        int daysInMonth = 0;
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            if (YearMonth.from(currentDate).equals(month)) {
                daysInMonth++;
            }
            currentDate = currentDate.plusDays(1);
        }
        return daysInMonth;
    }

    private LocalDate getStartOfWeek(LocalDate date) {
        int daysFromSunday = date.getDayOfWeek().getValue() % DayOfWeek.SUNDAY.getValue();
        return date.minusDays(daysFromSunday);
    }

    private Date toStartDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private Date toEndDate(LocalDate date) {
        return Date.from(date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).minusNanos(1).toInstant());
    }

    private record WeekRange(LocalDate startDate, LocalDate endDate) {
    }
}
