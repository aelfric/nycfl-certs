package org.nycfl.certificates;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Path("/enums")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class EnumResource {

    @GET
    @Path("event_types")
    public List<LabeledEnumDTO> getTypes(){
        return Arrays
            .stream(EventType.values())
            .map(LabeledEnumDTO::new)
            .collect(Collectors.toList());
    }

    @GET
    @Path("certificate_types")
    public List<LabeledEnumDTO> getCertTypes(){
        return Arrays
            .stream(CertificateType.values())
            .map(LabeledEnumDTO::new)
            .collect(Collectors.toList());
    }

    @GET
    @Path("elim_types")
    public List<LabeledEnumDTO> getRounds(){
        return Arrays
            .stream(EliminationRound.values())
            .map(LabeledEnumDTO::new)
            .collect(Collectors.toList());
    }

    public static class LabeledEnumDTO{
        public final String label;
        public final String value;

        @SuppressWarnings("CdiInjectionPointsInspection")
        public LabeledEnumDTO(LabeledEnum labeledEnum) {
            this.label = labeledEnum.getLabel();
            this.value = labeledEnum.getValue();
        }
    }
}
