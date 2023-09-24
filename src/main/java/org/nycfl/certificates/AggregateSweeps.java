package org.nycfl.certificates;

import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AggregateSweeps {
    public final Map<String, Map<Long, SweepsResult>> resultsMap;
    public final Map<String, Integer> totals;


    @JsonbCreator
    public AggregateSweeps(
            @JsonbProperty("resultsMap") Map<String, Map<Long, SweepsResult>> resultsMap,
            @JsonbProperty("totals") Map<String, Integer> totals) {
        this.resultsMap = resultsMap;
        this.totals = totals;
    }

    public AggregateSweeps(
            List<SweepsResult> results) {
        this.resultsMap =
                results.stream().collect(
                        Collectors.groupingBy(
                            SweepsResult::school,
                        Collectors.toMap(
                            SweepsResult::tournamentId,
                                Function.identity())));
        totals = new HashMap<>();
        for (Map.Entry<String, Map<Long, SweepsResult>> schoolResultMap :
                resultsMap
                .entrySet()) {
            Map<Long, SweepsResult> value = schoolResultMap.getValue();
            int total = value.values().stream().mapToInt(SweepsResult::points).sum();
            totals.put(schoolResultMap.getKey(), total);
        }
    }
}
