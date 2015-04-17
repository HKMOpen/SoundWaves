package org.bottiger.podcast.playlist;

import java.util.ArrayList;
import java.util.HashSet;

import org.bottiger.podcast.ApplicationConfiguration;
import org.bottiger.podcast.MainActivity;
import org.bottiger.podcast.adapters.PlaylistAdapter;
import org.bottiger.podcast.adapters.decoration.OnDragStateChangedListener;
import org.bottiger.podcast.provider.DatabaseHelper;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.ItemColumns;
import org.bottiger.podcast.provider.PodcastOpenHelper;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.provider.SubscriptionColumns;
import org.bottiger.podcast.service.PlayerService;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;

public class Playlist implements OnDragStateChangedListener {

    public static final boolean SHOW_LISTENED_DEFAULT = true;
    public static final boolean PLAY_NEXT_DEFAULT     = false;

	private static int MAX_SIZE = 100;
    private static Playlist activePlaylist = null;

    private static final String mSortNew = "DESC";
    private static final String mSortOld = "ASC";

    public enum SORT { DATE_NEW, DATE_OLD };

	private Context mContext;

    private HashSet<Long> mSubscriptions = new HashSet<>();
	private static ArrayList<FeedItem> mInternalPlaylist = new ArrayList<FeedItem>();
	private SharedPreferences sharedPreferences;

	// Shared setting key/values
	private final String showListenedKey = ApplicationConfiguration.showListenedKey;
	private boolean showListenedVal = SHOW_LISTENED_DEFAULT;
	private String inputOrderKey = "inputOrder";
	private String defaultOrder = mSortNew;
	private String amountKey = "amountOfEpisodes";
	private int amountValue = 20;

    private SORT mSortOrder = SORT.DATE_NEW;


	// http://stackoverflow.com/questions/1036754/difference-between-wait-and-sleep
	private static Boolean lock = true;

    private static HashSet<PlaylistChangeListener> sPlaylistChangeListeners = new HashSet<PlaylistChangeListener>();

	public Playlist(int length) {
		this(length, false);
	}

	public Playlist(int length, boolean isLocked) {
        if (activePlaylist == null) {
            activePlaylist = this;
        }
    }

	public Playlist() {
		this(MAX_SIZE);
	}

    public void setContext(@NonNull Context argContext) {

        if (mContext == null) {
            sharedPreferences = PreferenceManager
                    .getDefaultSharedPreferences(argContext);
            showListenedVal = sharedPreferences.getBoolean(showListenedKey, showListenedVal);
        }
        this.mContext = argContext;
    }

	/**
	 * @return The playlist as a list of episodes
	 */
	public ArrayList<FeedItem> getPlaylist() {
		return mInternalPlaylist;
	}

	/**
	 * 
	 * @return the size of the playlist
	 */
	public int size() {
		return mInternalPlaylist.size();
	}

    public int defaultSize() {
        return amountValue;
    }

	/**
	 * 
	 * @param position
	 *            in the playlist (0-indexed)
	 * @return The episode at the given position
	 */
	public FeedItem getItem(int position) {
        if (position >= mInternalPlaylist.size())
            return null;

		return mInternalPlaylist.get(position);
	}

    public FeedItem getNext() {
        return getItem(1);
    }

	/**
	 * 
	 * @param episode
	 * @return The position of the episode
	 */
	public int getPosition(FeedItem episode) {
		return mInternalPlaylist.indexOf(episode);
	}

	/**
	 * 
	 * @param position
	 * @param item
	 */
	public void setItem(int position, FeedItem item) {
		int size = mInternalPlaylist.size();
		if (size > position) {
            mInternalPlaylist.add(position, item);
        } else if (size == position) {
			mInternalPlaylist.add(item);
		}
	}

    /**
     *
     * @param position
     */
    public void removeItem(int position) {
        if (position < 0) {
            throw new IllegalArgumentException("Position must be greater or equal to zero"); // NoI18N
        }

        int size = mInternalPlaylist.size();
        if (size > position) {
            if (mInternalPlaylist.remove(position) != null) {
                notifyPlaylistRangeChanged(0, position);
            }
        }
    }

	/**
	 * 
	 * @param cursor
	 */
	public void setItem(Cursor cursor) {
		int position = cursor.getPosition();
		if (position < MAX_SIZE) {
			setItem(position, FeedItem.getByCursor(cursor));
		}
	}

    /**
     * When new items are fetched from a remote destination we can use this method
     * to notify the playlist about them instead of passing them back and forth in the database
     */
    public void notifyAbout(@NonNull FeedItem argEpisode) {

        // TODO: Expand this with filters
        int counter = 0;
        boolean isAfter;

        for (FeedItem episode : mInternalPlaylist) {
            isAfter = argEpisode.getDateTime().after(episode.getDateTime());
            if (isAfter) {
                final int size = mInternalPlaylist.size();

                if (size == MAX_SIZE) {
                    mInternalPlaylist.remove(mInternalPlaylist.size() - 1);
                }

                mInternalPlaylist.add(counter, argEpisode);

                notifyPlaylistRangeChanged(counter, size - 1);
                return;
            }
            counter++;
        }
    }

	/**
	 * @return The next item in the playlist
	 */
	public FeedItem nextEpisode() {
		if (mInternalPlaylist.size() > 1) {
            return mInternalPlaylist.get(1);
        }
		return null;
	}

	/**
	 * 
	 * @param from
	 *            , old position in the playlist
	 * @param to
	 *            , the new position in the playlist
	 */
	public void move(int from, int to) {
        populatePlaylistIfEmpty();

        FeedItem fromItem = mInternalPlaylist.get(from);
        mInternalPlaylist.remove(from);
        mInternalPlaylist.add(to, fromItem);

        int min = from;
        int max = to;

        if (to < from) {
            min = to;
            max = from;
        }

        notifyPlaylistRangeChanged(min-1, max-1);

        FeedItem precedingItem = to == 0 ? null : mInternalPlaylist.get(to-1);
        FeedItem movedItem = mInternalPlaylist.get(from);
        persist(mContext, movedItem, precedingItem, from, to);
	}

	/**
	 * 
	 *
	 *            of episodes
	 * @return A SQL formatted string of the order
	 */
	public String getOrder() {

		String inputOrder = sharedPreferences.getString(inputOrderKey,
				defaultOrder);
		int amount = sharedPreferences.getInt(amountKey, amountValue);

		PlayerService playerService = MainActivity.sBoundPlayerService;

		String playingFirst = "";
		if (playerService != null && playerService.getCurrentItem() != null) {
			playingFirst = "case " + ItemColumns.TABLE_NAME + "." + ItemColumns._ID + " when "
					+ playerService.getCurrentItem().getId()
					+ " then 1 else 2 end, ";
		}
		String prioritiesSecond = "case " + ItemColumns.TABLE_NAME + "." + ItemColumns.PRIORITY
				+ " when 0 then 2 else 1 end, " + ItemColumns.TABLE_NAME + "." + ItemColumns.PRIORITY + ", ";
		String order = playingFirst + prioritiesSecond + ItemColumns.TABLE_NAME + "." + ItemColumns.DATE + " "
				+ inputOrder + " LIMIT " + amount; // before:
		return order;
	}

	/**
	 * 
	 * @return A SQL formatted string of the where clause
	 */
	public String getWhere() {

        // show/hide listened episodes
		Boolean showListened = sharedPreferences.getBoolean(showListenedKey,
				showListenedVal);
		String where = (showListened) ? "1==1" : ItemColumns.LISTENED + "== 0";


        // only find episodes from suscriptions which are not "unsubscribed"
        where += " AND (";
        where += ItemColumns.TABLE_NAME + "." + ItemColumns.SUBS_ID + " IN (SELECT " + SubscriptionColumns.TABLE_NAME + "." + SubscriptionColumns._ID + " FROM "  +
                SubscriptionColumns.TABLE_NAME + " WHERE " + SubscriptionColumns.TABLE_NAME + "." + SubscriptionColumns.STATUS + "<>"
                + Subscription.STATUS_UNSUBSCRIBED + " OR " + SubscriptionColumns.TABLE_NAME + "." + SubscriptionColumns.STATUS + " IS NULL)";
        //where += ItemColumns.TABLE_NAME + "." + ItemColumns.SUBS_ID + " IN (4)";
        where += " )";

        // Limit the playlist to a fixed number of subscriptions
        String where3 = "";

        synchronized (mSubscriptions) {
            if (!mSubscriptions.isEmpty()) {

                where3 += " AND " + ItemColumns.SUBS_ID + " IN (";


                for (Long id : mSubscriptions) {
                    where3 += id + ",";
                }

                where3 = where3.substring(0, where3.length() - 1); // FIXME: ugly
                where3 += ")";

                where += where3;
            }

        }

        // skip 'removed' episodes
        where += " AND (" + ItemColumns.TABLE_NAME + "." + ItemColumns.PRIORITY + " >= 0)";


		return where;
        //return "1==1";
	}

	/**
	 * 
	 */
	public void resetPlaylist(CursorAdapter adapter) {
		// Update the database
		String currentTime = String.valueOf(System.currentTimeMillis());
		String updateLastUpdate = ", " + ItemColumns.LAST_UPDATE + "="
				+ currentTime + " ";

		// We remove the playlist position for all items in the playlist.
		String action = "UPDATE " + ItemColumns.TABLE_NAME + " SET ";
		String value = ItemColumns.PRIORITY + "=0" + updateLastUpdate;
		String where = "WHERE " + ItemColumns.PRIORITY + "<> 0";

		// Also update the timestamp of the top item in order to indicate to the
		// drivesyncer
		// Our data is up tp date.
		String where2 = " OR " + ItemColumns._ID + "==(select "
				+ ItemColumns._ID + " from " + ItemColumns.TABLE_NAME
				+ " order by " + ItemColumns.DATE + " desc limit 1)";

		String sql = action + value + where + where2;

		DatabaseHelper dbHelper = new DatabaseHelper();
		dbHelper.executeSQL(mContext, sql, adapter);
	}

	/**
	 * Populates the playlist up to a certain length if the playlist is empty
	 */
	public boolean populatePlaylistIfEmpty() {
		if (mInternalPlaylist.isEmpty()) {
            populatePlaylist(MAX_SIZE);
            return true;
        }
        return false;
	}

	/**
	 * Populates the playlist up to a certain length
	 * 
	 * @param length
	 *            of the playlist
	 */
    public void populatePlaylist(int length) {
        populatePlaylist(length, false);
    }

	public void populatePlaylist(int length, boolean force) {
        if (mInternalPlaylist.size() >= length && !force) {
            return;
        }

        if (mContext == null) {
            Log.e("PlaylistState", "Context can not be null!");
            throw new IllegalStateException("Context can not be null");
        }

        int previousSize = mInternalPlaylist.size();

        Cursor cursor = null;
        try {
            PodcastOpenHelper helper = PodcastOpenHelper.getInstance(mContext);//new PodcastOpenHelper(mContext);
            SQLiteDatabase database = helper.getReadableDatabase();

            cursor = database.query(ItemColumns.TABLE_NAME,
                    ItemColumns.ALL_COLUMNS, getWhere(), null, null, null,
                    getOrder());


        mInternalPlaylist.clear();
        cursor.moveToPosition(-1);

        while (cursor.moveToNext()) {
            setItem(cursor);
        }
        } catch (Exception lockedEx) { //FIXME
            return;
        } finally {
            if (cursor != null)
                cursor.close();
        }

        int newSize = mInternalPlaylist.size();

        // prevent infinite loop because "populateIfEmpty" will be called again and again
        if (previousSize == 0 && newSize == 0)
            return;

        notifyPlaylistChanged();
	}

    /**
     * Write changes to the playlist back to the database.
     * @param context
     * @param from
     * @param to
     */
    public static void persist(final Context context,
                        final FeedItem movedItem, final FeedItem precedingItem, final int from, final int to) {
        new Thread(new Runnable() {
            public void run() {

                if (from != to) {
                    movedItem.setPriority(precedingItem, context);
                }
            }
        }).start();
    }

    public boolean contains(FeedItem argItem) {
        return mInternalPlaylist.contains(argItem);
    }

    public boolean isEmpty() {
        return mInternalPlaylist.isEmpty();
    }

    public FeedItem first() {
        if (mInternalPlaylist.size() <= 0) {
            throw new IllegalStateException("Playlist is empty"); // NoI18N
        }
        return mInternalPlaylist.get(0);
    }

    public static Playlist getActivePlaylist() {
        if (activePlaylist == null) {
            throw new IllegalStateException("No Active Playlist"); // NoI18N
        }
        return activePlaylist;
    }

    public static void setActivePlaylist(@NonNull Playlist argPlaylist) {
        if (argPlaylist != activePlaylist) {
            activePlaylist = argPlaylist;
            activePlaylist.notifyPlaylistChanged();
        } else {
            throw new IllegalStateException("New playlist is the same");
        }
    }

    private int dragStart = -1;
    @Override
    public void onDragStart(int position) {
        dragStart = position+ PlaylistAdapter.PLAYLIST_OFFSET;
    }

    @Override
    public void onDragStop(int position) {
        move(dragStart, position+ PlaylistAdapter.PLAYLIST_OFFSET);
        dragStart = -1;
    }

    public interface PlaylistChangeListener {
        public void notifyPlaylistChanged();
        public void notifyPlaylistRangeChanged(int from, int to);
    }

    public synchronized void registerPlaylistChangeListener(@NonNull PlaylistChangeListener argChangeListener) {
        if (sPlaylistChangeListeners.contains(argChangeListener))
            return;

        sPlaylistChangeListeners.add(argChangeListener);
    }

    public synchronized void unregisterPlaylistChangeListener(@NonNull PlaylistChangeListener argChangeListener) {
        sPlaylistChangeListeners.remove(argChangeListener);
    }

    public void notifyDatabaseChanged() {
        populatePlaylist(amountValue, true);
        notifyPlaylistChanged();
    }

    public void notifyPlaylistChanged() {
        for (PlaylistChangeListener listener : sPlaylistChangeListeners) {
            if (listener == null) {
                throw new IllegalStateException("Listener can ot be null");
            }
            listener.notifyPlaylistChanged();
        }
    }

    public void notifyPlaylistRangeChanged(final int argFrom, final int argTo) {
        for (PlaylistChangeListener listener : sPlaylistChangeListeners) {
            if (listener == null) {
                throw new IllegalStateException("Listener can ot be null");
            }
            //listener.notifyPlaylistRangeChanged(argFrom, argTo);

            final PlaylistChangeListener finalListener = listener;
            //if (mContext instanceof Activity) {
            //    ((Activity)mContext).runOnUiThread(new Runnable() {
            //        @Override
            //        public void run() {
                        finalListener.notifyPlaylistRangeChanged(argFrom, argTo);
            //        }
            //    });
            //}
        }
    }

    public void setSortOrder(SORT argSortOrder) {
        boolean isChanged = mSortOrder != argSortOrder;
        mSortOrder = argSortOrder;

        if (isChanged) {
            String order = mSortOrder == SORT.DATE_NEW ? mSortNew : mSortOld;
            sharedPreferences.edit().putString(inputOrderKey, order).commit();
            notifyDatabaseChanged();
        }
    }

    public void setShowListened(boolean argShowListened) {
        boolean isChanged = showListenedVal != argShowListened;
        showListenedVal = argShowListened;

        if (isChanged) {
            sharedPreferences.edit().putBoolean(showListenedKey, argShowListened).commit();
            notifyDatabaseChanged();
        }
    }

    public void addSubscriptionID(Long argID) {
        if (mSubscriptions.contains(argID))
            return;

        mSubscriptions.add(argID);
    }

    public void clearSubscriptionID() {
        mSubscriptions.clear();
    }
}
