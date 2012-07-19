package info.bottiger.podcast;

import info.bottiger.podcast.R;
import info.bottiger.podcast.provider.FeedItem;
import info.bottiger.podcast.provider.ItemColumns;
import info.bottiger.podcast.provider.SubscriptionColumns;
import info.bottiger.podcast.utils.DialogMenu;
import info.bottiger.podcast.utils.IconCursorAdapter;

import java.util.HashMap;

import android.app.AlertDialog;
import android.app.Service;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

public class AllItemActivity extends PodcastBaseActivity {

	private static final int MENU_REFRESH = Menu.FIRST + 1;
	private static final int MENU_SORT = Menu.FIRST + 2;
	private static final int MENU_SELECT = Menu.FIRST + 3;

	private static final int MENU_ITEM_VIEW_CHANNEL = Menu.FIRST + 8;
	private static final int MENU_ITEM_DETAILS = Menu.FIRST + 9;
	private static final int MENU_ITEM_START_DOWNLOAD = Menu.FIRST + 10;
	private static final int MENU_ITEM_START_PLAY = Menu.FIRST + 11;
	private static final int MENU_ITEM_ADD_TO_PLAYLIST = Menu.FIRST + 12;
	
	private static final String[] PROJECTION = new String[] { 
		 	ItemColumns._ID, // 0
			ItemColumns.TITLE, // 1
			ItemColumns.DURATION, 
			ItemColumns.SUB_TITLE, 
			ItemColumns.STATUS,
			ItemColumns.SUBS_ID,
			ItemColumns.KEEP

	};

	private static HashMap<Integer, Integer> mIconMap;
	public static HashMap<Integer, Integer> mKeepIconMap;
	
	private long pref_order;
	private long pref_where;
	private long pref_select;
	/*
	private long pref_select_bits;	//bitmask of which status values to display
		private static long pref_select_bits_new = 1<<0;	//new or viewed
		private static long pref_select_bits_download = 1<<1; //being downloaded
		private static long pref_select_bits_unplayed = 1<<2; //downloaded, not in playlist
		private static long pref_select_bits_inplay = 1<<3;	//in playlist, play, pause
		private static long pref_select_bits_done = 1<<4; //done being played
		private static long pref_select_bits_all = -1;	//all bits set
	 */
	
	static {

		mIconMap = new HashMap<Integer, Integer>();
		initFullIconMap(mIconMap);
		
		mKeepIconMap = new HashMap<Integer, Integer>();
		mKeepIconMap.put(1, R.drawable.keep);		
		mKeepIconMap.put(IconCursorAdapter.ICON_DEFAULT_ID, R.drawable.blank);	 //anything other than KEEP	

	}
	
	public static void initFullIconMap(HashMap<Integer,Integer> iconMap) {
		iconMap.put(ItemColumns.ITEM_STATUS_UNREAD, R.drawable.feed_new);
		iconMap.put(ItemColumns.ITEM_STATUS_READ, R.drawable.feed_viewed);
		
		iconMap.put(ItemColumns.ITEM_STATUS_DOWNLOAD_PAUSE, R.drawable.download_pause);
		iconMap.put(ItemColumns.ITEM_STATUS_DOWNLOAD_QUEUE, R.drawable.download_wait);
		iconMap.put(ItemColumns.ITEM_STATUS_DOWNLOADING_NOW, R.drawable.downloading);
		
		iconMap.put(ItemColumns.ITEM_STATUS_NO_PLAY, R.drawable.playable);
		iconMap.put(ItemColumns.ITEM_STATUS_PLAY_READY, R.drawable.play_ready);
		iconMap.put(ItemColumns.ITEM_STATUS_PLAYING_NOW, R.drawable.playing);
		iconMap.put(ItemColumns.ITEM_STATUS_PLAY_PAUSE, R.drawable.play_pause);
		iconMap.put(ItemColumns.ITEM_STATUS_PLAYED, R.drawable.played);
		//iconMap.put(ItemColumns.ITEM_STATUS_KEEP, R.drawable.keep);
			//we now show KEEP status with a separate icon, based on separate DB flag
		
		iconMap.put(IconCursorAdapter.ICON_DEFAULT_ID, R.drawable.status_unknown);		//default for unknowns
	}
	
	public static IconCursorAdapter listItemCursorAdapter(Context context, Cursor cursor) {
		IconCursorAdapter.FieldHandler[] fields = {
				IconCursorAdapter.defaultTextFieldHandler,
				IconCursorAdapter.defaultTextFieldHandler,
				IconCursorAdapter.defaultTextFieldHandler,
				new IconCursorAdapter.IconFieldHandler(mIconMap),
				new IconCursorAdapter.IconFieldHandler(mKeepIconMap)
		};
		return new IconCursorAdapter(context, R.layout.list_item, cursor,
				new String[] { ItemColumns.TITLE, ItemColumns.SUB_TITLE,
						ItemColumns.DURATION, ItemColumns.STATUS, ItemColumns.KEEP },
				new int[] { R.id.text1, R.id.text2, R.id.text3, R.id.icon, R.id.keep_icon },
				fields);
	}

	public static IconCursorAdapter channelListItemCursorAdapter(Context context, Cursor cursor) {
		IconCursorAdapter.FieldHandler[] fields = {
				IconCursorAdapter.defaultTextFieldHandler,
				new IconCursorAdapter.IconFieldHandler(mIconMap),
				new IconCursorAdapter.IconFieldHandler(mKeepIconMap)
		};
		return new IconCursorAdapter(context, R.layout.channel_list_item, cursor,
				new String[] { ItemColumns.TITLE, ItemColumns.STATUS, ItemColumns.KEEP },
				new int[] { R.id.text1, R.id.icon, R.id.keep_icon },
				fields);
	}

	public static IconCursorAdapter channelListSubscriptionCursorAdapter(Context context, Cursor cursor) {
		IconCursorAdapter.FieldHandler[] fields = {
				IconCursorAdapter.defaultTextFieldHandler,
				new IconCursorAdapter.IconFieldHandler(mIconMap),
				new IconCursorAdapter.IconFieldHandler(mKeepIconMap)
		};
		return new IconCursorAdapter(context, R.layout.channel_list_item_old, cursor,
				new String[] { SubscriptionColumns.TITLE, SubscriptionColumns.COMMENT, SubscriptionColumns.LINK },
				new int[] { R.id.text1, R.id.icon, R.id.keep_icon },
				fields);
	}
	
	public static int mapToIcon(int status) {
		Integer iconI = mIconMap.get(status);
		if (iconI==null)
			iconI = mIconMap.get(IconCursorAdapter.ICON_DEFAULT_ID);	//look for default value in map
		int icon = (iconI!=null)?
			iconI.intValue():
			R.drawable.status_unknown;	//Use this icon when not in map and no map default.
				//This allows going back to a previous version after data has been
				//added in a new version with additional status codes.
		return icon;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);
		setTitle(getResources().getString(R.string.title_episodes));

		getListView().setOnCreateContextMenuListener(this);
		Intent intent = getIntent();
		intent.setData(ItemColumns.URI);
		
		mPrevIntent = new Intent(this, ChannelsActivity.class);
		mNextIntent = new Intent(this, DownloadingActivity.class);
		
		getPref();

		TabsHelper.setEpisodeTabClickListeners(this, R.id.episode_bar_all_button);

		startInit();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_REFRESH, 0,
				getResources().getString(R.string.menu_update)).setIcon(
				android.R.drawable.ic_menu_rotate);
		menu.add(0, MENU_SORT, 1,
				getResources().getString(R.string.menu_sort)).setIcon(
				android.R.drawable.ic_menu_agenda);	
		menu.add(0, MENU_SELECT, 2,
				getResources().getString(R.string.menu_select)).setIcon(
				android.R.drawable.ic_menu_today);			
		return true;
	}

	/*
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem item = menu.findItem(MENU_DISPLAY);
		String auto;
		if(pref_where==0){
			auto = "Only Undownload";
		}else{
			auto = "Display All";
		}        
        item.setTitle(auto);
        return true;
    }
    */
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_REFRESH:
			mServiceBinder.start_update();
			return true;
		case MENU_SORT:
			 new AlertDialog.Builder(this)
             .setTitle("Chose Sort Mode")
             .setSingleChoiceItems(R.array.sort_select, (int) pref_order, new DialogInterface.OnClickListener() {
                 public void onClick(DialogInterface dialog, int select) {
      			
         			if(mCursor!=null)
         				mCursor.close();
         			
         			pref_order = select;
         			SharedPreferences prefsPrivate = getSharedPreferences(Pref.HAPI_PREFS_FILE_NAME, Context.MODE_PRIVATE);
    				Editor prefsPrivateEditor = prefsPrivate.edit();
    				prefsPrivateEditor.putLong("pref_order", pref_order);
    				prefsPrivateEditor.commit();

         			mCursor = managedQuery(ItemColumns.URI, PROJECTION, getWhere(), null, getOrder());
         			mAdapter.changeCursor(mCursor);
         			//setListAdapter(mAdapter);         			
         			dialog.dismiss();

                 }
             })
            .show();
			return true;
		case MENU_SELECT:
			 new AlertDialog.Builder(this)
            .setTitle("Chose Select Mode")
            .setSingleChoiceItems(R.array.select_select, (int) pref_select, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int select) {
     			
        			if(mCursor!=null)
        				mCursor.close();
        			
        			pref_select = select;
        			SharedPreferences prefsPrivate = getSharedPreferences(Pref.HAPI_PREFS_FILE_NAME, Context.MODE_PRIVATE);
	   				Editor prefsPrivateEditor = prefsPrivate.edit();
	   				prefsPrivateEditor.putLong("pref_select", pref_select);
	   				prefsPrivateEditor.commit();

        			mCursor = managedQuery(ItemColumns.URI, PROJECTION, getWhere(), null, getOrder());
        			mAdapter.changeCursor(mCursor);
        			//setListAdapter(mAdapter);         			
        			dialog.dismiss();
                }
            })
           .show();
			return true;
		/*
		case MENU_DISPLAY:
 			if(mCursor!=null)
 				mCursor.close();
 			pref_where = 1- pref_where;

 			SharedPreferences prefsPrivate = getSharedPreferences(Pref.HAPI_PREFS_FILE_NAME, Context.MODE_PRIVATE);
			Editor prefsPrivateEditor = prefsPrivate.edit();
			prefsPrivateEditor.putLong("pref_where", pref_where);
			prefsPrivateEditor.commit();
			
 			mCursor = managedQuery(ItemColumns.URI, PROJECTION,getWhere(), null, getOrder());
 			mAdapter.changeCursor(mCursor);
 			return true;
		*/		
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {

		Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);
		String action = getIntent().getAction();
		if (Intent.ACTION_PICK.equals(action)
				|| Intent.ACTION_GET_CONTENT.equals(action)) {
			setResult(RESULT_OK, new Intent().setData(uri));
		} else {


			DialogMenu dialog_menu = createDialogMenus(id);
			if( dialog_menu==null)
				return;
			
			
			 new AlertDialog.Builder(this)
             .setTitle(dialog_menu.getHeader())
             .setItems(dialog_menu.getItems(), new MainClickListener(dialog_menu,id)).show();		

		}
	}
	
	public DialogMenu createDialogMenus(long id) {

		FeedItem feed_item = FeedItem.getById(getContentResolver(), id);
		if (feed_item == null) {
			return null;
		}
		
		DialogMenu dialog_menu = new DialogMenu();
		
		dialog_menu.setHeader(feed_item.title);
		
		dialog_menu.addMenu(MENU_ITEM_DETAILS, 
				getResources().getString(R.string.menu_details));
		dialog_menu.addMenu(MENU_ITEM_VIEW_CHANNEL, 
				getResources().getString(R.string.menu_view_channel));
		
		if(feed_item.status<ItemColumns.ITEM_STATUS_MAX_READING_VIEW){
			dialog_menu.addMenu(MENU_ITEM_START_DOWNLOAD, 
					getResources().getString(R.string.menu_download));			
		}else if(feed_item.status>ItemColumns.ITEM_STATUS_MAX_DOWNLOADING_VIEW){
			dialog_menu.addMenu(MENU_ITEM_START_PLAY, 
					getResources().getString(R.string.menu_play));
			dialog_menu.addMenu(MENU_ITEM_ADD_TO_PLAYLIST, 
					getResources().getString(R.string.menu_add_to_playlist));	
		}

		return dialog_menu;
	}	

	


	class MainClickListener implements DialogInterface.OnClickListener {
		public DialogMenu mMenu;
		public long item_id;
		public MainClickListener(DialogMenu menu, long id)
		{
			mMenu = menu;
			item_id = id;
		}
		
        public void onClick(DialogInterface dialog, int select) 
        {
    		switch (mMenu.getSelect(select)) {
    		case MENU_ITEM_DETAILS: {
    			FeedItem.view(AllItemActivity.this, item_id);
    			return;
    		} 
    		case MENU_ITEM_VIEW_CHANNEL: {
    			FeedItem.viewChannel(AllItemActivity.this, item_id);
    			return;
    		}  

    		case MENU_ITEM_START_DOWNLOAD: {
	
				FeedItem feeditem = FeedItem.getById(getContentResolver(), item_id);
				if (feeditem == null)
					return;
	
				feeditem.status = ItemColumns.ITEM_STATUS_DOWNLOAD_QUEUE;
				feeditem.update(getContentResolver());
				mServiceBinder.start_download();
				return;
			}
			case MENU_ITEM_START_PLAY: {
				FeedItem.play(AllItemActivity.this, item_id);
				return;
			}
			case MENU_ITEM_ADD_TO_PLAYLIST: {
				FeedItem.addToPlaylist(AllItemActivity.this, item_id);
				return;
			}
    		}
		}        	
        }
        


	@Override
	public void startInit() {

		mCursor = managedQuery(ItemColumns.URI, PROJECTION, getWhere(), null, getOrder());

		mAdapter = AllItemActivity.listItemCursorAdapter(this, mCursor);
		setListAdapter(mAdapter);

		super.startInit();

	}
	public String getOrder() {
			String order = ItemColumns.CREATED + " DESC";
 			if(pref_order==0){
 				 order = ItemColumns.SUBS_ID +"," +order;
 			}else if(pref_order==1){
				 order = ItemColumns.STATUS +"," +order;
 			}
 			return order;
	}	

	public String getWhere() {
		String where = ItemColumns.STATUS + "<" + ItemColumns.ITEM_STATUS_MAX_PLAYLIST_VIEW;
		switch ((int)pref_select) {
		case 1:		// New only
			where =  ItemColumns.STATUS + "<" + ItemColumns.ITEM_STATUS_MAX_READING_VIEW;
			break;
		case 2:		// Unplayed only
			where =  ItemColumns.STATUS + "=" + ItemColumns.ITEM_STATUS_NO_PLAY;
			break;
		case 3:		// Playable only
			where = "(" + where + ") AND (" + 
					ItemColumns.STATUS + ">" + ItemColumns.ITEM_STATUS_MAX_DOWNLOADING_VIEW + ")";
			break;
		default:	// case 0 = All, no change to initial where clause
			;	// treat any unknown values as "All"
		}
		return where;
	}	
	
	public void getPref() {
		SharedPreferences pref = getSharedPreferences(
				Pref.HAPI_PREFS_FILE_NAME, Service.MODE_PRIVATE);
		pref_order = pref.getLong("pref_order",2);
		pref_where = pref.getLong("pref_where", 0);
		pref_select = pref.getLong("pref_select", 0);
	}
}