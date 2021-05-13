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
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.Collections;

public class YoutubeResource {
  private static final String CLIENT_SECRETS= "client_secret.json";
  private static final Collection<String> SCOPES =
      Collections.singletonList("https://www.googleapis.com/auth/youtube");

  private static final String APPLICATION_NAME = "NYCFL Certificates";
  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

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

    final LiveBroadcast liveBroadcast = scheduleStream(youtubeService,
        "NYCFL Live - Final Round Duo",
        DateTime.parseRfc3339("2021-02-08T15:32:26.553Z"),
        DateTime.parseRfc3339("2021-02-08T17:32:26.553Z"));


  }

  private static LiveBroadcast scheduleStream(YouTube youtubeService, String streamTitle, DateTime startTime, DateTime endTime) throws IOException {
    final LiveBroadcastSnippet snippet = new LiveBroadcastSnippet();

    snippet.setTitle(streamTitle);
    snippet.setScheduledStartTime(startTime);
    snippet.setScheduledEndTime(endTime);

    final LiveBroadcastStatus status = new LiveBroadcastStatus();
    status.setPrivacyStatus("private");
    status.setMadeForKids(false);

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
