package org.nycfl.certificates;

import io.quarkus.qute.Template;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.nycfl.certificates.slides.SlideBuilder;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Path("/certs")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CertificatesResource {
    @Inject
    TournamentService tournamentService;

    @Inject
    Template cert;

    @GET
    @Path("/background.svg")
    @Produces("image/svg+xml")
    public String getBackgroundImage(
        @QueryParam("color") @DefaultValue("ffffff") String color
    ) {
        return cert.data("color", "#" + color).render();
    }


    @POST
    @Path("/tournaments")
    public Tournament createTournament(Tournament tournament) {

        return tournamentService.createTournament(tournament);
    }

    @POST
    @Path("/tournaments/{id}")
    @Transactional
    public Tournament updateTournament(
            @PathParam("id") long tournamentId,
            Tournament tournament) {
        return tournamentService.updateTournament(tournamentId, tournament);
    }

    @POST
    @Path("/events")
    public Tournament createEvents(EventList eventList) {
        return tournamentService.addEvents(eventList);
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/tournaments/{tournamentId}/events/{eventId}/results")
    public Tournament addElimResults(@MultipartForm MultipartBody body,
                                     @PathParam("eventId") int eventId,
                                     @PathParam("tournamentId") long tournamentId,
                                     @QueryParam("type") @DefaultValue(
                                             "FINALIST") EliminationRound eliminationRound) {

        return tournamentService.addResults(
                eventId,
                tournamentId,
                eliminationRound,
                body.file);
    }
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/tournaments/{tournamentId}/events/{eventId}/results")
    public Tournament clearResults(@PathParam("eventId") int eventId,
                                   @PathParam("tournamentId") long tournamentId) {
        return tournamentService.clearResults(eventId);
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/tournaments/{id}/sweeps")
    public Tournament addSweepsResults(@MultipartForm MultipartBody body,
                                       @PathParam("id") long tournamentId) {
        Map<String, School> map =
                tournamentService.getSchools(tournamentId).stream().collect(
                        Collectors.toMap(School::getDisplayName,
                                Function.identity()));
        try {
            CSVParser parse = CSVParser.parse(body.file, StandardCharsets.UTF_8,
                    CSVFormat.DEFAULT.withFirstRecordAsHeader());
            for (CSVRecord record : parse.getRecords()) {
                School school = map.computeIfAbsent(
                        record.get("School"),
                        School::fromName);
                school.setSweepsPoints(
                        Integer.parseInt(
                                record.get("Total")));
                tournamentService.updateSchool(school, tournamentId);
            }
        } catch (IOException e) {
            throw new BadRequestException("");
        }
        return tournamentService.getTournament(tournamentId);
    }

    @GET
    @Path("/tournaments")
    public List<Tournament> listAllTournaments() {
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
            @MultipartForm MultipartBody body) {
        Map<String, School> map =
                tournamentService.getSchools(tournamentId).stream().collect(
                        Collectors.toMap(School::getName, Function.identity()));
        try {
            CSVParser parse = CSVParser.parse(body.file, StandardCharsets.UTF_8,
                    CSVFormat.DEFAULT.withFirstRecordAsHeader());
            for (CSVRecord record : parse.getRecords()) {
                School school = map.computeIfAbsent(
                        record.get("Short Name"),
                        School::fromName);
                school.setDisplayName(record.get("Full Name"));
                ;
                tournamentService.updateSchool(school, tournamentId);
            }
        } catch (IOException e) {
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
        return tournamentService
                .updateEventType(eventId, eventType);
    }

    @POST
    @Path("/tournaments/{id}/events/{evtId}/placement")
    public Tournament setPlacementCutoff(
            @PathParam("id") long tournamentId,
            @PathParam("evtId") long eventId,
            CutoffRequest cutoffRequest
    ) {
        return tournamentService
                .updatePlacementCutoff(eventId, cutoffRequest.cutoff);
    }

    @POST
    @Path("/tournaments/{id}/events/{evtId}/cutoff")
    public Tournament setCertificateCutoff(
            @PathParam("id") long tournamentId,
            @PathParam("evtId") long eventId,
            CutoffRequest cutoffRequest
    ) {
        return tournamentService
                .updateCertificateCutoff(eventId, cutoffRequest.cutoff);
    }

    @POST
    @Path("/tournaments/{id}/events/{evtId}/medal")
    public Tournament setMedalCutoff(
            @PathParam("id") long tournamentId,
            @PathParam("evtId") long eventId,
            CutoffRequest cutoffRequest
    ) {
        return tournamentService
                .updateMedalCutoff(eventId, cutoffRequest.cutoff);
    }

    @Inject
    Template certificate;

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/tournaments/{id}/certificates")
    public String generateCertificates(@PathParam("id") long tournamentId) {
        Tournament tournament = tournamentService.getTournament(tournamentId);
        return certificate
                .data("tournament", tournament)
                .render();
    }

    @Inject
    SlideBuilder slideBuilder;

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/tournaments/{id}/slides")
    public Response generateSlidesPreview(
        @PathParam("id") long tournamentId,
        @QueryParam("dl") @DefaultValue("0") int download
    ) {
        Tournament tournament = tournamentService.getTournament(tournamentId);

        if(download==1){
            return Response
                .ok(slideBuilder.buildSlidesFile(tournament))
                .header("Content-Disposition","attachment; filename=\"slides_tournament_"+tournamentId+".zip\"")
                .build();
        } else {
            return Response.ok(slideBuilder.buildSlidesPreview(tournament)).build();
        }
    }

    @GET
    @Path("/tournaments/{id}/medals")
    public List<MedalCount> getMedalCount(@PathParam("id") long tournamentId) {
        return tournamentService.getMedalCount(tournamentId);
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
}