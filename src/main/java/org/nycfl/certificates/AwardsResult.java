package org.nycfl.certificates;

import javax.json.bind.annotation.JsonbCreator;
import java.util.ArrayList;
import java.util.List;

public class AwardsResult {
    public final String studentName;
    public final int place;
    public final String schoolName;
    public final String eventName;
    public final EliminationRound eliminationRound;
    public final EventType eventType;
    public final String award;

    private AwardsResult(String studentName,
                         AwardsResult that){
        this.studentName = studentName;
        this.place = that.place;
        this.schoolName = that.schoolName;
        this.eventName = that.eventName;
        this.eliminationRound = that.eliminationRound;
        this.eventType = that.eventType;

        this.award = that.award;
    }

    @JsonbCreator
    public AwardsResult(Result result,
                        String schoolName,
                        String eventName,
                        EventType eventType) {
        this.studentName = result.getName();
        this.place = result.getPlace();
        this.eliminationRound = result.getEliminationRound();
        this.schoolName = schoolName;
        this.eventName = eventName;
        this.eventType = eventType;
        this.award = this.eventType.formatPlacementString(result);
    }

    public List<AwardsResult> split() {
        String[] names = this.studentName.split(" & ");
        ArrayList<AwardsResult> list = new ArrayList<>();
        for (String name : names) {
            list.add(new AwardsResult(name.strip(), this));
        }
        return  list;
    }
}
