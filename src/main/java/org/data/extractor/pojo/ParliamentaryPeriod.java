package org.data.extractor.pojo;

public record ParliamentaryPeriod(
        String name,
        int startYear,
        int endYear,
        boolean isActive
) {
}
