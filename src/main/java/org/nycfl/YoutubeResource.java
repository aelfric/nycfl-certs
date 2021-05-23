package org.nycfl;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Path("/youtube")
@RolesAllowed({"basicuser,streamcontroller"})
public class YoutubeResource {
  private static final String CLIENT_SECRETS = "/credentials.json";
  private static final Collection<String> SCOPES =
      Collections.singletonList("https://www.googleapis.com/auth/youtube");

  private static final String APPLICATION_NAME = "NYCFL Certificates";
  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

  @ConfigProperty(name="google.credentials.path")
  String googleCredentialsPath;

  public static class LiveStreamRequest {
    public String title;
    public String startTime;
    public String endTime;
  }

  @GET
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public List<LiveStreamResponse> getStreams() {
    try {
      YouTube youtubeService = getService();
      return getScheduledStreams(youtubeService);
    } catch (GeneralSecurityException | IOException e) {
      throw new InternalServerErrorException("Could not connect to YouTube", e);
    }
  }

  @POST
  @RolesAllowed({"streamcontroller"})
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public List<LiveStreamResponse> scheduleStreams(List<LiveStreamRequest> details) {
    try {
      YouTube youtubeService = getService();
      for (LiveStreamRequest schedule : details) {
        createYoutubeStream(
            youtubeService,
            schedule.title,
            DateTime.parseRfc3339(schedule.startTime),
            DateTime.parseRfc3339(schedule.endTime)
        );
      }
      return getScheduledStreams(youtubeService);
    } catch (GeneralSecurityException | IOException e) {
      throw new InternalServerErrorException("Could not connect to YouTube", e);
    }
  }

  @POST
  @Path("/{broadcastId}/complete")
  @RolesAllowed({"streamcontroller"})
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public List<LiveStreamResponse> completeStream(@PathParam("broadcastId") String broadcastId) {
    try {
      final YouTube service = getService();
      transition(BroadcastStatus.COMPLETE, broadcastId, service);
      return getScheduledStreams(service);
    } catch (IOException | GeneralSecurityException e) {
      throw new BadRequestException("Could not transition the stream", e);
    }
  }

  @POST
  @RolesAllowed({"streamcontroller"})
  @Path("/{broadcastId}/golive")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public List<LiveStreamResponse> goLive(@PathParam("broadcastId") String broadcastId) {
    try {
      final YouTube service = getService();
      transition(BroadcastStatus.LIVE, broadcastId, service);
      return getScheduledStreams(service);
    } catch (IOException | GeneralSecurityException e) {
      throw new BadRequestException("Could not transition the stream", e);
    }
  }

  @POST
  @RolesAllowed({"streamcontroller"})
  @Path("/{broadcastId}/test")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public List<LiveStreamResponse> testStream(@PathParam("broadcastId") String broadcastId) {
    try {
      final YouTube service = getService();
      transition(BroadcastStatus.TESTING, broadcastId, service);
      return getScheduledStreams(service);
    } catch (IOException | GeneralSecurityException e) {
      throw new BadRequestException("Could not transition the stream", e);
    }
  }

  /**
   * Create an authorized Credential object.
   *
   * @return an authorized Credential object.
   * @throws IOException if the credentials file or data store cannot be found
   */
  private Credential authorize(final NetHttpTransport httpTransport) throws IOException {
    // Load client secrets.
    InputStream in = YoutubeResource.class.getResourceAsStream(CLIENT_SECRETS);
    GoogleClientSecrets clientSecrets =
        GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
    // Build flow and trigger user authorization request.
    final File dataDirectory = new File(googleCredentialsPath);
    GoogleAuthorizationCodeFlow flow =
        new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
            .setDataStoreFactory(new FileDataStoreFactory(dataDirectory))
            .build();
    return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
  }

  /**
   * Build and return an authorized API client service.
   *
   * @return an authorized API client service
   * @throws GeneralSecurityException, IOException
   */
  private YouTube getService() throws GeneralSecurityException, IOException {
    final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    Credential credential = authorize(httpTransport);
    return new YouTube.Builder(httpTransport, JSON_FACTORY, credential)
        .setApplicationName(APPLICATION_NAME)
        .build();
  }

  private static List<LiveStreamResponse> getScheduledStreams(YouTube youtubeService) throws IOException {
    YouTube.LiveStreams.List request = youtubeService.liveStreams()
        .list("snippet,cdn,status");
    LiveStreamListResponse execute =
        request
            .setMine(true)
            .setMaxResults(64L)
            .execute();
    Map<String, LiveStream> streamMap = execute
        .getItems()
        .stream()
        .collect(Collectors.toMap(LiveStream::getId, Function.identity()));

    YouTube.LiveBroadcasts.List request2 = youtubeService.liveBroadcasts().list("snippet,status,contentDetails");
    final LiveBroadcastListResponse broadcastListResponse =
        request2
            .setMine(true)
            .setMaxResults(64L)
            .execute();
    final List<LiveStreamResponse> liveStreamResponses = new ArrayList<>();
    for (LiveBroadcast broadcast : broadcastListResponse.getItems()) {
      final LiveStreamResponse liveStreamResponse = getLiveStreamResponse(streamMap, broadcast);
      liveStreamResponses.add(liveStreamResponse);
    }
    return liveStreamResponses;
  }

  private static LiveStreamResponse getLiveStreamResponse(Map<String, LiveStream> streamMap, LiveBroadcast item) {
    final String boundStreamId = item.getContentDetails().getBoundStreamId();

    return LiveStreamResponse
        .builder()
        .withBroadcast(item)
        .withLiveStream(streamMap.get(boundStreamId))
        .build();
  }

  private static void createYoutubeStream(YouTube youtubeService, String streamTitle, DateTime startTime, DateTime endTime) throws IOException {
    final LiveBroadcastSnippet snippet = new LiveBroadcastSnippet();

    snippet.setTitle(streamTitle);
    snippet.setScheduledStartTime(startTime);
    snippet.setScheduledEndTime(endTime);

    final LiveBroadcastStatus status = new LiveBroadcastStatus();
    status.setPrivacyStatus("private");
    status.setSelfDeclaredMadeForKids(false);

    final LiveBroadcast broadcast = new LiveBroadcast();
    broadcast.setKind("youtube#liveBroadcast");
    broadcast.setSnippet(snippet);
    broadcast.setStatus(status);

    final YouTube.LiveBroadcasts.Insert broadcastInsert = youtubeService.liveBroadcasts().insert("snippet,status", broadcast);
    final LiveBroadcast returnedBroadcast = broadcastInsert.execute();

    final LiveStreamSnippet streamSnippet = new LiveStreamSnippet();
    streamSnippet.setTitle(streamTitle);
    final CdnSettings cdnSettings = new CdnSettings();
    cdnSettings.setFormat("1080p");
    cdnSettings.setIngestionType("rtmp");
    cdnSettings.setFrameRate("variable");
    cdnSettings.setResolution("variable");

    final LiveStream liveStream = new LiveStream();
    liveStream.setKind("youtube#liveStream");
    liveStream.setSnippet(streamSnippet);
    liveStream.setCdn(cdnSettings);

    final YouTube.LiveStreams.Insert liveStreamInsert = youtubeService.liveStreams().insert("snippet,cdn", liveStream);
    final LiveStream returnedStream = liveStreamInsert.execute();

    final YouTube.LiveBroadcasts.Bind liveBroadcastBind = youtubeService.liveBroadcasts().bind(returnedBroadcast.getId(), "id,contentDetails");
    liveBroadcastBind.setStreamId(returnedStream.getId());

    liveBroadcastBind.execute();
  }

  public static void transition(BroadcastStatus broadcastStatus, String broadcastId, YouTube youtubeService) throws GeneralSecurityException, IOException {
    // Define and execute the API request
    YouTube.LiveBroadcasts.Transition request = youtubeService
        .liveBroadcasts()
        .transition(
            broadcastStatus.value,
            broadcastId,
            "snippet,status"
        );
    request.execute();
  }

  public enum BroadcastStatus {
    TESTING("testing"),
    LIVE("live"),
    COMPLETE("complete");

    public final String value;

    BroadcastStatus(String value) {
      this.value = value;
    }
  }
}
