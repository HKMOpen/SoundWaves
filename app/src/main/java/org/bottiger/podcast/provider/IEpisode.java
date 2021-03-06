package org.bottiger.podcast.provider;

import android.content.ContentResolver;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.net.URL;
import java.util.Date;

/**
 * Created by apl on 21-04-2015.
 */
public interface IEpisode {

    String getTitle();
    URL getUrl();
    @Nullable String getArtwork(@NonNull Context argContext);
    String getDescription();
    String getAuthor();
    long getDuration();
    int getPriority();
    ISubscription getSubscription(@NonNull Context argContext);
    long getOffset();
    Date getDateTime();

    void setTitle(@NonNull String argTitle);
    void setUrl(@NonNull URL argUrl);
    void setArtwork(@NonNull URL argUrl);
    void setDescription(@NonNull String argDescription);
    void setDuration(long argDurationMs);
    void setOffset(@Nullable ContentResolver contentResolver, long i);

    /**
     * -1 => nothing
     * 0 => current playing item
     * 1 => first item in the playlist
     * 2 => second item in the playist
     *
     * @param argPriority
     */
    void setPriority(int argPriority);
    void setPriority(IEpisode argPrecedingItem, @NonNull Context argContext);

    boolean isDownloaded();

    void update(ContentResolver contentResolver);

}
