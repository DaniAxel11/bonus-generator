package com.truper.bonusgenerator.service.bonus;

import com.truper.bonusgenerator.model.dto.request.CommitReportRequest;
import com.truper.bonusgenerator.model.dto.response.CommitReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BonusServiceImpl implements BonusService {

    @Override
    public CommitReportResponse generateReport(CommitReportRequest request) {
        return null;
    }
}
