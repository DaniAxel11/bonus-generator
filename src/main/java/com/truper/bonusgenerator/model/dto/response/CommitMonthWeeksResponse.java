package com.truper.bonusgenerator.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommitMonthWeeksResponse {

    private int year;
    private int month;
    private int totalWeeks;
    private List<CommitWeekResponse> weeks;
}
