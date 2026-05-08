package org.nycfl.certificates.lastround;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import jakarta.annotation.security.RolesAllowed;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.nycfl.certificates.Event;
import org.nycfl.certificates.Tournament;
import org.nycfl.certificates.lastround.EventMappingRepository.EventMappingDTO;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/certs")
@RolesAllowed({"basicuser", "superuser"})
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class LastRoundResource {
    private final EntityManager em;
    private final LastRoundRepository importRepository;
    private final EventMappingRepository eventMappingRepository;

    public LastRoundResource(EntityManager em, LastRoundRepository importRepository, EventMappingRepository eventMappingRepository) {
        this.em = em;
        this.importRepository = importRepository;
        this.eventMappingRepository = eventMappingRepository;
    }

    @Transactional
    @POST
    @RolesAllowed("superuser")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/tournaments/{tournamentId}/bulk_results")
    public LastRoundImport addElimResults(@RestForm("file") FileUpload body,
                                          @PathParam("tournamentId") long tournamentId) {
        LastRoundImport.Builder builder = LastRoundImport.builder(
            em.getReference(Tournament.class, tournamentId)
        );
        try (var is = Files.newInputStream(body.uploadedFile())) {

            CsvMapper mapper = new CsvMapper();
            mapper
                .readerFor(StagedResult.class)
                .with(CsvSchema.emptySchema().withHeader())
                .<StagedResult>readValues(is)
                .forEachRemaining(
                    builder::addRow
                );
            LastRoundImport anImport = builder.build();
            importRepository.persist(anImport);
            return anImport;
        } catch (IOException e) {
            throw new BadRequestException("Could not process results", e);
        }
    }

    @GET
    @RolesAllowed("superuser")
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/tournaments/{tournamentId}/bulk_results/{reportId}/mappings")
    public List<EventMappingDTO> getElimMappings(
        @PathParam("tournamentId") long tournamentId,
        @PathParam("reportId") long reportId
    ) {
        return this.eventMappingRepository.findByReportId(reportId);
    }

    @GET
    @RolesAllowed("superuser")
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/tournaments/{tournamentId}/bulk_results/{reportId}/mappings/{mappingId}")
    public EventMappingDTO getRoundMappings(
        @PathParam("tournamentId") long tournamentId,
        @PathParam("reportId") long reportId,
        @PathParam("mappingId") long mappingId
    ) {
        return this.eventMappingRepository.findDtoById(mappingId).orElseThrow();
    }

    @Transactional
    @POST
    @RolesAllowed("superuser")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/tournaments/{tournamentId}/bulk_results/{reportId}/mappings/events")
    public List<EventMappingDTO> setElimEventMappings(
        @PathParam("tournamentId") long tournamentId,
        @PathParam("reportId") long reportId,
        Map<String, Long> mapping
    ) {
        Map<String, Event> parsedMapping = mapping.entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey,
            e -> em.getReference(Event.class, e.getValue())
        ));
        LastRoundImport report = this.importRepository.findById(reportId);
        this.importRepository.persist(report.applyEventMapping(parsedMapping));
        return this.eventMappingRepository.findByReportId(reportId);
    }

}
