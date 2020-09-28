package org.nycfl.certificates;

import io.quarkus.qute.Template;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Path("/certs")
public class CertificatesResource {
    @Inject
    TournamentService tournamentService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/tournaments")
    public Tournament createTournament(Tournament tournament){

        return tournamentService.createTournament(tournament);
    }
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/events")
    public Tournament createEvents(EventList eventList){
        return tournamentService.addEvents(eventList);
    }
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/results")
    public Tournament addResults(@MultipartForm MultipartBody body,
                                 @QueryParam("eventId") int eventId,
                                 @QueryParam("tournamentId") long tournamentId){
        System.out.println("Received " + body.fileName);
        List<Result> results = new ArrayList<>();
        Map<String, School> map =
                tournamentService.getSchools(tournamentId).stream().collect(
                        Collectors.toMap(School::getName, Function.identity()));
        try {
            CSVParser parse = CSVParser.parse(body.file, StandardCharsets.UTF_8, CSVFormat.DEFAULT.withFirstRecordAsHeader());
            List<String> headerNames = parse.getHeaderNames();
            for (CSVRecord record : parse.getRecords()) {
                Result result = new Result();
                if(headerNames.contains("Name 2")){
                    result.name = record.get("Name 1") + " & " + record.get("Name 2");
                } else {
                    result.name = record.get("Name 1");
                }
                result.code = record.get("Code");
                result.place = Integer.parseInt(record.get("Ranking"));
                result.school = map.computeIfAbsent(
                        record.get("School"),
                        School::fromName);
                results.add(result);
            }
        } catch (IOException e){
            throw new BadRequestException("");
        }
        tournamentService.addSchools(map.values(), tournamentId);
        return tournamentService.addResults(eventId, results);
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/tournaments/{id}/sweeps")
    public Tournament addSweepsResults(@MultipartForm MultipartBody body,
                                       @PathParam("id") long tournamentId){
        Map<String, School> map =
                tournamentService.getSchools(tournamentId).stream().collect(
                        Collectors.toMap(School::getDisplayName, Function.identity()));
        try {
            CSVParser parse = CSVParser.parse(body.file, StandardCharsets.UTF_8, CSVFormat.DEFAULT.withFirstRecordAsHeader());
            for (CSVRecord record : parse.getRecords()) {
                School school = map.computeIfAbsent(
                    record.get("School"),
                    School::fromName);
                school.setSweepsPoints(
                    Integer.parseInt(
                        record.get("Total")));
                tournamentService.updateSchool(school, tournamentId);
            }
        } catch (IOException e){
            throw new BadRequestException("");
        }
        return tournamentService.getTournament(tournamentId);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/tournaments")
    public List<Tournament> listAllTournaments(){
        return tournamentService.all();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/tournaments/{id}/schools")
    public List<School> listSchools(@PathParam("id") long tournamentId){
        return tournamentService.getSchools(tournamentId);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/tournaments/{id}/schools")
    public List<School> addSchools(
        @PathParam("id") long tournamentId,
        @MultipartForm MultipartBody body){
        Map<String, School> map =
            tournamentService.getSchools(tournamentId).stream().collect(
                Collectors.toMap(School::getName, Function.identity()));
        try {
            CSVParser parse = CSVParser.parse(body.file, StandardCharsets.UTF_8, CSVFormat.DEFAULT.withFirstRecordAsHeader());
            for (CSVRecord record : parse.getRecords()) {
                School school = map.computeIfAbsent(
                    record.get("Short Name"),
                    School::fromName);
                school.setDisplayName(record.get("Full Name"));;
                tournamentService.updateSchool(school, tournamentId);
            }
        } catch (IOException e){
            throw new BadRequestException("");
        }
        return new ArrayList<>(map.values());
    }
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/tournaments/{id}/events/{evtId}/placement")
    public Tournament setPlacementCutoff(
            @PathParam("id") long tournamentId,
            @PathParam("evtId") long eventId,
            CutoffRequest cutoffRequest
            ){
        return tournamentService.updatePlacementCutoff(eventId, cutoffRequest.cutoff);
    }
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/tournaments/{id}/events/{evtId}/cutoff")
    public Tournament setCertificateCutoff(
            @PathParam("id") long tournamentId,
            @PathParam("evtId") long eventId,
            CutoffRequest cutoffRequest
            ){
        return tournamentService.updateCertificateCutoff(eventId, cutoffRequest.cutoff);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/tournaments/{id}/events/{evtId}/medal")
    public Tournament setMedalCutoff(
            @PathParam("id") long tournamentId,
            @PathParam("evtId") long eventId,
            CutoffRequest cutoffRequest
            ){
        return tournamentService.updateMedalCutoff(eventId, cutoffRequest.cutoff);
    }

    @Inject
    Template certificate;

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/tournaments/{id}/certificates")
    public String generateCertificates(@PathParam("id") long tournamentId){
        StringBuilder output = new StringBuilder();
        output.append("<link rel='stylesheet' href='/certs.css' />");
        Tournament tournament = tournamentService.getTournament(tournamentId);
        return certificate
            .data("tournament", tournament)
            .render();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/tournaments/{id}/medals")
    public List<MedalCount> getMedalCount(@PathParam("id") long tournamentId){
        return tournamentService.getMedalCount(tournamentId);
    }
}