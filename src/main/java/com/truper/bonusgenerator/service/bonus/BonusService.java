package com.truper.bonusgenerator.service.bonus;

import com.truper.bonusgenerator.model.dto.request.CommitReportRequest;
import com.truper.bonusgenerator.model.dto.response.CommitReportResponse;

public interface BonusService {

    CommitReportResponse generateReport(CommitReportRequest request);

}
