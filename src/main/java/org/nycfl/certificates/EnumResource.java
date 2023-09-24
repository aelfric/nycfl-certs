package org.nycfl.certificates;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.List;

@Path("/enums")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class EnumResource {

    @GET
    @Path("event_types")
    public List<LabeledEnumDTO> getTypes(){
        return Arrays
            .stream(EventType.values())
            .map(LabeledEnumDTO::fromLabeledEnum)
            .toList();
    }

    @GET
    @Path("certificate_types")
    public List<LabeledEnumDTO> getCertTypes(){
        return Arrays
            .stream(CertificateType.values())
            .map(LabeledEnumDTO::fromLabeledEnum)
            .toList();
    }

    @GET
    @Path("elim_types")
    public List<LabeledEnumDTO> getRounds(){
        return Arrays
            .stream(EliminationRound.values())
            .map(LabeledEnumDTO::fromLabeledEnum)
            .toList();
    }

    public record LabeledEnumDTO(String label, String value){
      static LabeledEnumDTO fromLabeledEnum(LabeledEnum e){
        return new LabeledEnumDTO(e.getLabel(), e.getValue());
      }
    }
}
