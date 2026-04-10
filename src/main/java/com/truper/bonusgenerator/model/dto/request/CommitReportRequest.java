package com.truper.bonusgenerator.model.dto.request;

import com.truper.bonusgenerator.model.dto.CommitDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommitReportRequest {

    private List<CommitDto> commits;

}

