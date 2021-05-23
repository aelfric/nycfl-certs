package org.nycfl;

import com.google.api.services.youtube.model.LiveBroadcast;
import com.google.api.services.youtube.model.LiveBroadcastSnippet;
import com.google.api.services.youtube.model.LiveStream;

@SuppressWarnings("CdiInjectionPointsInspection")
public class LiveStreamResponse {
  public final String title;
  public final String startTime;
  public final String endTime;
  public final String streamKey;
  public final String status;
  public final String streamStatus;
  public final String monitorHtml;
  public final String broadcastId;
  public final String ingestionAddress;
  public final String privacyStatus;

  public LiveStreamResponse(
      String title,
      String startTime,
      String endTime,
      String streamKey,
      String status,
      String streamStatus,
      String monitorHtml,
      String broadcastId,
      String ingestionAddress,
      String privacyStatus) {
    this.title = title;
    this.startTime = startTime;
    this.endTime = endTime;
    this.streamKey = streamKey;
    this.status = status;
    this.streamStatus = streamStatus;
    this.monitorHtml = monitorHtml;
    this.broadcastId = broadcastId;
    this.ingestionAddress = ingestionAddress;
    this.privacyStatus = privacyStatus;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    public String title;
    public String startTime;
    public String endTime;
    public String streamKey;
    public String status;
    private String streamStatus;
    private String embedHtml;
    private String broadcastId;
    private String ingestionAddress;
    private String privacyStatus;

    public LiveStreamResponse build() {
      return new LiveStreamResponse(
          title,
          startTime,
          endTime,
          streamKey,
          status,
          streamStatus,
          embedHtml,
          broadcastId,
          ingestionAddress,
          privacyStatus
      );
    }

    public Builder withLiveStream(LiveStream liveStream) {
      if (liveStream != null) {
        streamKey = liveStream.getCdn().getIngestionInfo().getStreamName();
        ingestionAddress = liveStream.getCdn().getIngestionInfo().getIngestionAddress();
        streamStatus = liveStream.getStatus().getStreamStatus();
      }
      return this;
    }

    public Builder withBroadcast(LiveBroadcast broadcast) {
      this.broadcastId = broadcast.getId();
      final LiveBroadcastSnippet snippet = broadcast.getSnippet();
      this.title = snippet.getTitle();
      this.startTime = snippet.getScheduledStartTime().toStringRfc3339();
      this.endTime = snippet.getScheduledEndTime().toStringRfc3339();
      this.status = broadcast.getStatus().getLifeCycleStatus();
      this.embedHtml = broadcast.getContentDetails().getMonitorStream().getEmbedHtml();
      privacyStatus = broadcast.getStatus().getPrivacyStatus();
      return this;
    }
  }
}
