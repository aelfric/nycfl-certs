package org.nycfl.certificates;

import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public record AggregateSweeps(Map<String, Map<Long, SweepsResult>> resultsMap, Map<String, Integer> totals) {
    @JsonbCreator
    public AggregateSweeps(
        @JsonbProperty("resultsMap") Map<String, Map<Long, SweepsResult>> resultsMap,
        @JsonbProperty("totals") Map<String, Integer> totals) {
        this.resultsMap = resultsMap;
        this.totals = totals;
    }

    public AggregateSweeps(
        List<SweepsResult> results) {
        this(results.stream().collect(
            Collectors.groupingBy(
                SweepsResult::school,
                Collectors.toMap(
                    SweepsResult::tournamentId,
                    Function.identity()))), new HashMap<>());
        for (Map.Entry<String, Map<Long, SweepsResult>> schoolResultMap :
            resultsMap
                .entrySet()) {
            Map<Long, SweepsResult> value = schoolResultMap.getValue();
            int total = value.values().stream().mapToInt(SweepsResult::points).sum();
            totals.put(schoolResultMap.getKey(), total);
        }
    }
}
