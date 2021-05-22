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

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Path("/youtube")
public class YoutubeResource {
  private static final String CLIENT_SECRETS= "/credentials.json";
  private static final Collection<String> SCOPES =
      Collections.singletonList("https://www.googleapis.com/auth/youtube");

  private static final String APPLICATION_NAME = "NYCFL Certificates";
  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public List scheduleStreams(List details){
    return details;
  }

  /**
   * Create an authorized Credential object.
   *
   * @return an authorized Credential object.
   * @throws IOException
   */
  private static Credential authorize(final NetHttpTransport httpTransport) throws IOException {
    // Load client secrets.
    InputStream in = YoutubeResource.class.getResourceAsStream(CLIENT_SECRETS);
    GoogleClientSecrets clientSecrets =
        GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
    // Build flow and trigger user authorization request.
    GoogleAuthorizationCodeFlow flow =
        new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
            .setDataStoreFactory(new FileDataStoreFactory(new File("c:\\Users" +
                "\\aelfr" +
                "\\google")))
            .build();
    return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
  }

  /**
   * Build and return an authorized API client service.
   *
   * @return an authorized API client service
   * @throws GeneralSecurityException, IOException
   */
  private static YouTube getService() throws GeneralSecurityException, IOException {
    final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    Credential credential = authorize(httpTransport);
    return new YouTube.Builder(httpTransport, JSON_FACTORY, credential)
        .setApplicationName(APPLICATION_NAME)
        .build();
  }

  /**
   * Call function to create API service object. Define and
   * execute API request. Print API response.
   *
   * @throws GeneralSecurityException, IOException, GoogleJsonResponseException
   */
  public static void main(String[] args)
      throws GeneralSecurityException, IOException {
    YouTube youtubeService = getService();
    // Define and execute the API request

//    scheduleStream(youtubeService,
//        "NCFL Live - Final Round Lincoln Douglas Debate",
//        DateTime.parseRfc3339("2021-05-30T21:00:00.00Z"),
//        DateTime.parseRfc3339("2021-05-30T22:30:00.00Z"));
//
//  scheduleStream(youtubeService,
//        "NCFL Live - Final Round Public Forum Debate",
//      DateTime.parseRfc3339("2021-05-30T21:00:00.00Z"),
//      DateTime.parseRfc3339("2021-05-30T22:30:00.00Z"));
//
//  scheduleStream(youtubeService,
//        "NCFL Live - Final Round Duo",
//      DateTime.parseRfc3339("2021-05-30T21:00:00.00Z"),
//      DateTime.parseRfc3339("2021-05-30T22:30:00.00Z"));

    System.out.println(getScheduledStreams(youtubeService));

  }

  private static LiveStreamListResponse getScheduledStreams(YouTube youtubeService) throws IOException {
    YouTube.LiveStreams.List request = youtubeService.liveStreams()
        .list("snippet,cdn,status");
    LiveStreamListResponse execute = request.setMine(true).execute();
    for (LiveStream item : execute.getItems()) {
      System.out.println(item.getSnippet().getTitle());
      System.out.println(item.getCdn().getIngestionInfo().getStreamName());
    }
    return execute;
  }

  private static LiveBroadcast scheduleStream(YouTube youtubeService, String streamTitle, DateTime startTime, DateTime endTime) throws IOException {
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

    return liveBroadcastBind.execute();
  }
}
