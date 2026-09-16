package org.nycfl.certificates;

import io.quarkus.qute.Template;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.nycfl.certificates.slides.PostingsBuilder;
import org.nycfl.certificates.slides.SlideBuilder;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Path("/certs")
@RolesAllowed({"basicuser", "superuser"})
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CertificatesResource {

    private static final Logger LOG = Logger.getLogger(CertificatesResource.class);


    private final TournamentService tournamentService;
    private final Template certBorder;
    private final Template certificate;
    private final SlideBuilder slideBuilder;
    private final PostingsBuilder postingsBuilder;
    private final SecurityIdentity identity;

    @Inject
    public CertificatesResource(TournamentService tournamentService, Template certBorder, Template certificate, SlideBuilder slideBuilder, PostingsBuilder postingsBuilder, SecurityIdentity identity) {
        this.tournamentService = tournamentService;
        this.certBorder = certBorder;
        this.certificate = certificate;
        this.slideBuilder = slideBuilder;
        this.postingsBuilder = postingsBuilder;
        this.identity = identity;
    }

    @GET
    @Path("/background.svg")
    @Produces("image/svg+xml")
    @PermitAll
    public String getBackgroundImage(
        @QueryParam("color") @DefaultValue("ffffff") String color,
        @QueryParam("color2") @DefaultValue("323131") String color2,
        @QueryParam("color3") @DefaultValue("323131") String color3
    ) {
        return certBorder
            .data("color", "#" + color)
            .data("color2", "#" + color2)
            .data("color3", "#" + color3)
            .render();
    }

    @POST
    @Path("/tournaments")
    @RolesAllowed("superuser")
    public Tournament createTournament(Tournament tournament, @QueryParam("sourceId") long srcTournamentId) {

        if (srcTournamentId > 0) {
            return tournamentService.copyTournament(srcTournamentId);
        } else {
            if (tournament != null) {
                return tournamentService.createTournament(tournament);
            } else {
                throw new BadRequestException("No tournament stub provided");
            }
        }
    }

    @POST
    @Path("/tournaments/{id}")
    @Transactional
    public Tournament updateTournament(
        @PathParam("id") long tournamentId,
        Tournament tournament) {
        requireCircuitAccess(tournamentId);
        return tournamentService.updateTournament(tournamentId, tournament);
    }

    @RolesAllowed("superuser")
    @PUT
    @Path("/tournaments/{id}")
    @Transactional
    public Tournament assignTournament(
        @PathParam("id") long tournamentId,
        @QueryParam("circuit") String circuit
    ) {
        return tournamentService.assignTournamentToCircuit(tournamentId, circuit);
    }

    @GET
    @Path("/tournaments/{id}")
    public Tournament getTournament(
        @PathParam("id") long tournamentId) {
        return tournamentService.getTournament(tournamentId);
    }

    @POST
    @Path("/events")
    public Tournament createEvents(EventList eventList) {
        requireCircuitAccess(eventList.tournamentId());
        return tournamentService.addEvents(eventList);
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/tournaments/{tournamentId}/events/{eventId}/results")
    public Tournament addElimResults(@RestForm("file") FileUpload body,
                                     @PathParam("eventId") int eventId,
                                     @PathParam("tournamentId") long tournamentId,
                                     @QueryParam("type") @DefaultValue(
                                         "FINALIST") EliminationRound eliminationRound) {

        requireCircuitAccess(tournamentId);
        try (var is = Files.newInputStream(body.uploadedFile())) {
            return tournamentService.addResults(
                eventId,
                tournamentId,
                eliminationRound,
                is
            );
        } catch (IOException e) {
            throw new BadRequestException("Could not process results", e);
        }
    }

    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/tournaments/{tournamentId}/events/{eventId}/results")
    public Tournament clearResults(@PathParam("tournamentId") long tournamentId, @PathParam("eventId") int eventId) {
        requireCircuitAccess(tournamentId);
        return tournamentService.clearResults(eventId);
    }

    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/tournaments/{tournamentId}/events/{eventId}")
    public Tournament deleteEvent(@PathParam("tournamentId") long tournamentId, @PathParam("eventId") int eventId) {
        requireCircuitAccess(tournamentId);
        return tournamentService.deleteEvent(eventId);
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/tournaments/{id}/sweeps")
    public Tournament addSweepsResults(@RestForm("file") FileUpload body,
                                       @PathParam("id") long tournamentId) {
        requireCircuitAccess(tournamentId);
        Map<String, School> map =
            tournamentService.getSchools(tournamentId).stream().collect(
                Collectors.toMap(School::getDisplayName,
                    Function.identity()));
        try (var is = Files.newInputStream(body.uploadedFile())) {
            CSVParser parse = CSVUtils.parse(is);
            for (CSVRecord csvRecord : parse.getRecords()) {
                School school = map.computeIfAbsent(
                    csvRecord.get("School"),
                    School::fromName);
                school.setSweepsPoints(
                    Integer.parseInt(
                        csvRecord.get("Total")));
                tournamentService.updateSchool(school, tournamentId);
            }
        } catch (IOException _) {
            throw new BadRequestException("");
        }
        return tournamentService.getTournament(tournamentId);
    }

    @GET
    @Path("/tournaments")
    public List<TournamentStub> listAllTournaments() {
        return tournamentService.all();
    }

    @GET
    @Path("/tournaments/{id}/schools")
    public List<School> listSchools(@PathParam("id") long tournamentId) {
        return tournamentService.getSchools(tournamentId);
    }

    @DELETE
    @Path("/tournaments/{id}/schools/{schoolId}")
    public List<School> deleteSchool(@PathParam("id") long tournamentId,
                                     @PathParam("schoolId") long schoolId) {
        return tournamentService.deleteSchool(tournamentId, schoolId);
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/tournaments/{id}/schools")
    public List<School> addSchools(
        @PathParam("id") long tournamentId,
        @RestForm("file") FileUpload body) {
        requireCircuitAccess(tournamentId);
        Map<String, School> map =
            tournamentService.getSchools(tournamentId).stream().collect(
                Collectors.toMap(School::getName, Function.identity()));
        try (var is = Files.newInputStream(body.uploadedFile())) {
            CSVParser parse = CSVUtils.parse(is);
            for (CSVRecord csvRecord : parse.getRecords()) {
                School school = map.computeIfAbsent(
                    csvRecord.get("Short Name"),
                    School::fromName);
                school.setDisplayName(csvRecord.get("Full Name"));
                tournamentService.updateSchool(school, tournamentId);
            }
        } catch (IOException _) {
            throw new BadRequestException("");
        }
        return new ArrayList<>(map.values());
    }

    @POST
    @Path("/tournaments/{id}/events/{evtId}/type")
    public Tournament setEventType(
        @PathParam("id") long tournamentId,
        @PathParam("evtId") long eventId,
        @QueryParam("type") EventType eventType
    ) {
        requireCircuitAccess(tournamentId);
        return tournamentService
            .updateEventType(eventId, eventType);
    }

    @POST
    @Path("/tournaments/{id}/events/{evtId}/rename")
    public Tournament renameEvent(
        @PathParam("id") long tournamentId,
        @PathParam("evtId") long eventId,
        @QueryParam("name") String newName
    ) {
        requireCircuitAccess(tournamentId);
        return tournamentService
            .renameEvent(eventId, newName);
    }

    @POST
    @Path("/tournaments/{id}/events/{evtId}/abbreviate")
    public Tournament abbreviateEvent(
        @PathParam("id") long tournamentId,
        @PathParam("evtId") long eventId,
        @QueryParam("abbreviation") String abbreviation
    ) {
        requireCircuitAccess(tournamentId);
        return tournamentService
            .abbreviateEvent(eventId, abbreviation);
    }

    @POST
    @Path("/tournaments/{id}/events/{evtId}/results/{resultId}/rename")
    public Tournament renameCompetitor(
        @PathParam("id") long tournamentId,
        @PathParam("evtId") long eventId,
        @PathParam("resultId") long resultId,
        @QueryParam("name") @DefaultValue("") String newName
    ) {
        requireCircuitAccess(tournamentId);
        return tournamentService
            .renameCompetitor(eventId, resultId, newName);
    }

    @POST
    @Path("/tournaments/{id}/events/{evtId}/results/{resultId}/school")
    public Tournament switchCompetitorSchool(
        @PathParam("id") long tournamentId,
        @PathParam("evtId") long eventId,
        @PathParam("resultId") long resultId,
        @QueryParam("schoolId") long newSchool
    ) {
        requireCircuitAccess(tournamentId);
        return tournamentService.switchSchool(eventId, resultId, newSchool);
    }

    @POST
    @Path("/tournaments/{id}/events/{evtId}/rounds")
    public Tournament setNumRounds(
        @PathParam("id") long tournamentId,
        @PathParam("evtId") long eventId,
        @QueryParam("count") int count
    ) {
        requireCircuitAccess(tournamentId);
        return tournamentService
            .updateNumRounds(eventId, count);
    }

    @POST
    @Path("/tournaments/{id}/events/{evtId}/cert_type")
    public Tournament setCertificateType(
        @PathParam("id") long tournamentId,
        @PathParam("evtId") long eventId,
        @QueryParam("type") CertificateType certificateType
    ) {
        requireCircuitAccess(tournamentId);
        return tournamentService
            .updateCertificateType(eventId, certificateType);
    }

    @POST
    @Path("/tournaments/{id}/events/{evtId}/placement")
    public Tournament setPlacementCutoff(
        @PathParam("id") long tournamentId,
        @PathParam("evtId") long eventId,
        CutoffRequest cutoffRequest
    ) {
        requireCircuitAccess(tournamentId);
        return tournamentService
            .updatePlacementCutoff(eventId, cutoffRequest.cutoff());
    }

    @POST
    @Path("/tournaments/{id}/events/{evtId}/cutoff")
    public Tournament setCertificateCutoff(
        @PathParam("id") long tournamentId,
        @PathParam("evtId") long eventId,
        CutoffRequest cutoffRequest
    ) {
        requireCircuitAccess(tournamentId);
        return tournamentService
            .updateCertificateCutoff(eventId, cutoffRequest.cutoff());
    }

    @POST
    @Path("/tournaments/{id}/events/{evtId}/medal")
    public Tournament setMedalCutoff(
        @PathParam("id") long tournamentId,
        @PathParam("evtId") long eventId,
        CutoffRequest cutoffRequest
    ) {
        requireCircuitAccess(tournamentId);
        return tournamentService
            .updateMedalCutoff(eventId, cutoffRequest.cutoff());
    }

    @POST
    @Path("/tournaments/{id}/events/{evtId}/slide_size")
    public Tournament setEntriesPerSlide(
        @PathParam("id") long tournamentId,
        @PathParam("evtId") long eventId,
        CutoffRequest cutoffRequest
    ) {
        requireCircuitAccess(tournamentId);
        return tournamentService
            .updateEntriesPerSlide(eventId, cutoffRequest.cutoff());
    }

    @POST
    @Path("/tournaments/{id}/events/{evtId}/quals")
    public Tournament setHalfQuals(
        @PathParam("id") long tournamentId,
        @PathParam("evtId") long eventId,
        CutoffRequest cutoffRequest
    ) {
        requireCircuitAccess(tournamentId);
        return tournamentService
            .updateHalfQuals(eventId, cutoffRequest.cutoff());
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.TEXT_HTML)
    @Path("/tournaments/{id}/certificates")
    public String generateCertificates(@PathParam("id") long tournamentId) {
        Tournament tournament = tournamentService.getTournament(tournamentId);
        return certificate
            .data("tournament", tournament)
            .render();
    }

    @GET
    @Produces("text/csv")
    @Consumes(MediaType.TEXT_HTML)
    @PermitAll
    @Path("/tournaments/{id}/certificates/index")
    public String generateCertificatesIndex(@PathParam("id") long tournamentId) {
        Tournament tournament = tournamentService.getTournament(tournamentId);

        long startPage = 1;
        StringWriter out = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(out, CSVFormat.EXCEL)) {
            printer.printRecord("event", "startPage", "endPage");
            for (Event event : tournament.events) {
                if (event.getCertificateCutoff() > 0) {
                    long endPage = startPage + event.countCertificates() - 1;
                    printer.printRecord(event.getName(), startPage, endPage);
                    startPage = endPage + 1;
                }
            }
        } catch (IOException ex) {
            LOG.error("Could not generate index file", ex);
        }
        return out.toString();
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.TEXT_HTML)
    @Path("/tournaments/{id}/slides")
    public String generateSlidesPreview(
        @PathParam("id") long tournamentId,
        @QueryParam("dl") @DefaultValue("0") int download
    ) {
        Tournament tournament = tournamentService.getTournament(tournamentId);

        return slideBuilder.buildSlidesPreview(tournament);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.TEXT_HTML)
    @PermitAll
    @Path("/tournaments/{id}/postings")
    public String generatePostingsPreview(
        @PathParam("id") long tournamentId,
        @QueryParam("dl") @DefaultValue("0") int download
    ) {
        Tournament tournament = tournamentService.getTournament(tournamentId);

        return postingsBuilder.buildSlidesPreview(tournament);
    }

    @GET
    @Path("/tournaments/{id}/medals")
    public List<MedalCount> getMedalCount(@PathParam("id") long tournamentId) {
        return tournamentService.getMedalCount(tournamentId);
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/tournaments/{id}/contacts")
    public String uploadContactInfo(@RestForm("file") FileUpload body,
                                    @PathParam("id") long tournamentId) {
        requireCircuitAccess(tournamentId);
        try (var is = Files.newInputStream(body.uploadedFile())) {
            return "\"%d records updated\"".formatted(
                tournamentService.updateSchoolContacts(is)
            );
        } catch (IOException e) {
            throw new BadRequestException("Could not update contacts", e);
        }
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/tournaments/{id}/contacts")
    public List<School> getContactInfo(@PathParam("id") long tournamentId) {
        requireCircuitAccess(tournamentId);
        return tournamentService.getSchools(tournamentId);
    }

    @GET
    @Path("/tournaments/sweeps")
    public AggregateSweeps getSweeps() {
        return tournamentService.getSweeps();
    }

    @GET
    @Path("/tournaments/{id}/sweeps")
    public List<SweepsResult> getSweeps(@PathParam("id") long tournamentId) {
        return tournamentService.getSweeps(tournamentId);
    }

    private void requireCircuitAccess(long tournamentId) {
        Tournament t = tournamentService.getTournament(tournamentId);
        if (!identity.hasRole("superuser") && !identity.hasRole("manage-circuit-" + t.getCircuitId())) {

            throw new ForbiddenException(
                Response.status(403, "FORBIDDEN")
                    .entity("You are not permitted to manage circuit " + t.getCircuitId())
                    .build()
            );
        }
    }
}