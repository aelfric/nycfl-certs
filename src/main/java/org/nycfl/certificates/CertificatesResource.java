package org.nycfl.certificates;

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

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "hello";
    }

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
    public Tournament createTournament(EventList eventList){
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
            for (CSVRecord record : parse.getRecords()) {
                Result result = new Result();
                result.name = record.get("Name");
                result.code = record.get("Code");
                result.count = Integer.parseInt(record.get("Count"));
                result.place = Integer.parseInt(record.get("Place"));
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

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/tournaments")
    public List<Tournament> listAllTournaments(){
        return tournamentService.all();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/tournaments/{id}/schools")
    public List<School> list(@PathParam("id") long tournamentId){
        return tournamentService.getSchools(tournamentId);
    }
}