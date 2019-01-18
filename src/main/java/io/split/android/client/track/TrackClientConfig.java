package io.split.android.client.track;

 public class TrackClientConfig {

     private int maxQueueSize;
     private int maxEventsPerPost;
     private long flushIntervalMillis;
     private int waitBeforeShutdown;
     private int maxSentAttempts;

     public int getMaxQueueSize() {
         return maxQueueSize;
     }

     public void setMaxQueueSize(int maxQueueSize) {
         this.maxQueueSize = maxQueueSize;
     }

     public int getMaxEventsPerPost() {
         return maxEventsPerPost;
     }

     public void setMaxEventsPerPost(int maxEventsPerPost) {
         this.maxEventsPerPost = maxEventsPerPost;
     }

     public long getFlushIntervalMillis() {
         return flushIntervalMillis;
     }

     public void setFlushIntervalMillis(long flushIntervalMillis) {
         this.flushIntervalMillis = flushIntervalMillis;
     }

     public int hasToWaitBeforeShutdown() {
         return waitBeforeShutdown;
     }

     public void setWaitBeforeShutdown(int waitBeforeShutdown) {
         this.waitBeforeShutdown = waitBeforeShutdown;
     }

     public int getMaxSentAttempts() {
         return maxSentAttempts;
     }

     public void setMaxSentAttempts(int maxSentAttempts) {
         this.maxSentAttempts = maxSentAttempts;
     }
 }
