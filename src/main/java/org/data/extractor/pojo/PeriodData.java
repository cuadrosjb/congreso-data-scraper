package org.data.extractor.pojo;

import java.time.LocalDateTime;
import java.util.List;

record PeriodData(
        String periodName,
        LocalDateTime fetchedAt,
        List<CongressMember> members
) {}