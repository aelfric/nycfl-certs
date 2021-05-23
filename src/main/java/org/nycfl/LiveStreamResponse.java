package org.nycfl;

import com.google.api.services.youtube.model.LiveBroadcast;
import com.google.api.services.youtube.model.LiveBroadcastSnippet;
import com.google.api.services.youtube.model.LiveStream;

public class LiveStreamResponse {
  public final String title;
  public final String startTime;
  public final String endTime;
  public final String streamKey;
  public final String status;
  public final String streamStatus;
  public final String monitorHtml;
  public final String broadcastId;

  public LiveStreamResponse(String title, String startTime, String endTime, String streamKey, String status, String streamStatus, String monitorHtml, String broadcastId) {
    this.title = title;
    this.startTime = startTime;
    this.endTime = endTime;
    this.streamKey = streamKey;
    this.status = status;
    this.streamStatus = streamStatus;
    this.monitorHtml = monitorHtml;
    this.broadcastId = broadcastId;
  }

  public static Builder builder(){
    return new Builder();
  }

  public static class Builder{
    public String title;
    public String startTime;
    public String endTime;
    public String streamKey;
    public String status;
    private String streamStatus;
    private String embedHtml;
    private String broadcastId;

    public LiveStreamResponse build(){
      return new LiveStreamResponse(
          title,
          startTime,
          endTime,
          streamKey,
          status,
          streamStatus,
          embedHtml,
          broadcastId
      );
    }

    public Builder withLiveStream(LiveStream liveStream){
      if (liveStream != null) {
        streamKey = liveStream.getCdn().getIngestionInfo().getStreamName();
        streamStatus = liveStream.getStatus().getStreamStatus();
      }
      return this;
    }
    public Builder withBroadcast(LiveBroadcast broadcast){
      this.broadcastId = broadcast.getId();
      final LiveBroadcastSnippet snippet = broadcast.getSnippet();
      this.title = snippet.getTitle();
      this.startTime  = snippet.getScheduledStartTime().toStringRfc3339();
      this.endTime  = snippet.getScheduledEndTime().toStringRfc3339();
      this.status = broadcast.getStatus().getLifeCycleStatus();
      this.embedHtml = broadcast.getContentDetails().getMonitorStream().getEmbedHtml();
      return this;
    }
  }
}
