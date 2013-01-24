package info.bottiger.podcast.parser;

import info.bottiger.podcast.provider.FeedItem;
import info.bottiger.podcast.provider.ItemColumns;
import info.bottiger.podcast.provider.Subscription;
import info.bottiger.podcast.utils.Log;
import info.bottiger.podcast.utils.StrUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.xml.sax.SAXException;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.BaseColumns;

import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndEntry;
import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndFeed;
import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndImage;
import com.google.code.rome.android.repackaged.com.sun.syndication.io.FeedException;
import com.google.code.rome.android.repackaged.com.sun.syndication.io.SyndFeedInput;
import com.google.code.rome.android.repackaged.com.sun.syndication.io.XmlReader;

/**
 * 
 * @author Arvid Böttiger
 * 
 */
public class FeedParserWrapper {

	private final Log log = Log.getLog(getClass());
	private ContentResolver cr;

	private static FeedItem mostRecentItem = null;
	private String subscriptionTitle = null;
	private String subscriptionDescription = null;
	private String subscriptionImage = null;

	public FeedParserWrapper(ContentResolver cr) {
		this.cr = cr;
	}

	public void parse(Subscription subscription) {

		// try {
		jsonParser(subscription, mostRecentItem);
		/*
		 * If we can't parse the feed with RSSFeed we try with ROME
		 */
		/*
		 * } catch (Exception e) { e.printStackTrace(); romeParser(subscription,
		 * mostRecentItem); }
		 */

	}

	private void jsonParser(Subscription subscription, FeedItem recentItem) {

		String baseURL = "http://mygpo-feedservice.appspot.com/parse?url=";
		String extraHeader = "Accept: application/json";
		SimpleDateFormat dt = new SimpleDateFormat(FeedItem.default_format);

		URL url;
		try {
			url = new URL(baseURL + subscription.getURL().toString());
			URLConnection urlConnection = url.openConnection();
			urlConnection.setRequestProperty("Accept", "application/json");

			BufferedReader in = new BufferedReader(new InputStreamReader(
					urlConnection.getInputStream()));

			Object rootObject = JSONValue.parse(in);
			JSONArray mainArray = (JSONArray) rootObject;
			JSONObject mainDataObject = (JSONObject) mainArray.get(0);

			String image = "";
			if (mainDataObject.get("logo") != null)
				image = mainDataObject.get("logo").toString();

			JSONArray episodeDataObject = (JSONArray) mainDataObject
					.get("episodes");
			for (int i = 0; i < episodeDataObject.size(); i++) {
				FeedItem item = new FeedItem();

				JSONObject episode = (JSONObject) episodeDataObject.get(i);
				Number duration = (Number) episode.get("duration");

				JSONArray fileData = (JSONArray) episode.get("files");
				JSONObject files = (JSONObject) fileData.get(0);
				item.type = (String) files.get("mimetype");
				Number filesize = (Number) files.get("filesize");

				Number released = (Number) episode.get("released");
				Date time = null;
				if (released != null)
					time = new Date(released.longValue() * 1000);

				if (time != null)
					item.date = dt.format(time);
				if (duration != null)
					item.duration = StrUtils
							.formatTime(duration.intValue() * 1000);
				if (filesize != null)
					item.filesize = filesize.intValue();
				item.image = image;
				item.url = (String) episode.get("link");
				item.resource = item.url;

				item.title = (String) episode.get("title");
				item.author = (String) episode.get("author");
				item.content = (String) episode.get("description");

				updateFeed(subscription, item);

			}

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Parses a Feed with ROME. This is very robust, but also very slow.
	 * 
	 * @param subscription
	 * @param recentItem
	 */
	private void romeParser(Subscription subscription, FeedItem recentItem) {

		XmlReader reader = null;

		try {

			reader = new XmlReader(subscription.getURL());
			SyndFeed feed = new SyndFeedInput().build(reader);
			// System.out.println("Feed Title: " + feed.getAuthor());

			subscriptionTitle = feed.getTitle();
			subscriptionDescription = feed.getDescription();

			SyndImage image = feed.getImage();
			subscriptionImage = (image != null) ? image.getUrl() : "";

			for (Iterator i = feed.getEntries().iterator(); i.hasNext();) {
				SyndEntry entry = (SyndEntry) i.next();
				FeedItem currentItem = fromRSSEntry(entry);

				if (currentItem != null) {
					updateFeed(subscription, currentItem);
				}

				// System.out.println(entry.getTitle());
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (FeedException e) {
			e.printStackTrace();
		} finally {
			if (reader != null)
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}

	}

	/*
	 * private FeedItem fromRSSEntry(Item entry) { FeedItem item = new
	 * FeedItem();
	 * 
	 * SimpleDateFormat dt = new SimpleDateFormat(item.default_format);
	 * 
	 * item.author = entry.getAuthor(); item.title = entry.getTitle();
	 * item.content = (entry.getDescription() != null) ? entry
	 * .getDescription().toString() : ""; item.date =
	 * dt.format(entry.getPubDate()); item.resource = entry.getUri(); item.url =
	 * entry.getLink();
	 * 
	 * return item; }
	 */

	private FeedItem fromRSSEntry(SyndEntry entry) {
		FeedItem item = new FeedItem();

		SimpleDateFormat dt = new SimpleDateFormat(item.default_format);

		item.author = entry.getAuthor();
		item.title = entry.getTitle();
		item.content = (entry.getDescription() != null) ? entry
				.getDescription().toString() : "";
		item.date = dt.format(entry.getPublishedDate());
		item.resource = entry.getUri();
		item.url = entry.getLink();

		return item;
	}

	private FeedItem checkItem(FeedItem item) throws SAXException {
		if (item.title == null)
			item.title = "(Untitled)";
		item.title = strip(item.title);

		if (item.resource == null) {
			// log.warn("item have not a resource link: " + item.title);
			return null;
		}
		if (item.author == null)
			item.author = "(Unknown)";

		if (item.content == null)
			item.content = "(No content)";

		SimpleDateFormat correctRSSFormatter = new SimpleDateFormat(
				"EEE, dd MMM yyyy HH:mm:ss Z");
		DateFormat wrongRSSFormatter = new SimpleDateFormat(
				"dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
		String ff = ItemColumns.DATE_FORMAT;
		SimpleDateFormat correctSQLFormatter = new SimpleDateFormat(ff);

		// In case the date is in a wrong format
		Date date = null;
		try {
			date = correctRSSFormatter.parse(item.date);
		} catch (ParseException e) {
			try {
				date = wrongRSSFormatter.parse(item.date);
			} catch (ParseException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		if (item.getDate() == 0) {

			date = new Date();
			// item.date = correctFormatter.format(date);
			// log.warn("item.date: " + item.date);
		}

		if (date != null)
			item.date = correctSQLFormatter.format(date);

		return item;
	}

	private String strip(String str) {

		Pattern pattern = Pattern.compile("\\n");
		Matcher matcher = pattern.matcher(str);
		str = matcher.replaceAll("");

		pattern = Pattern.compile("\\s+");
		matcher = pattern.matcher(str);
		str = matcher.replaceAll(" ");

		pattern = Pattern.compile("^\\s+");
		matcher = pattern.matcher(str);
		str = matcher.replaceAll("");

		pattern = Pattern.compile("\\s+$");
		matcher = pattern.matcher(str);
		str = matcher.replaceAll("");

		return str;
	}

	public int updateFeed(Subscription subscription, FeedItem item) {
		long update_date = subscription.lastItemUpdated;
		int add_num = 0;

		// Get most recent Item for comparison
		if (FeedParserWrapper.mostRecentItem == null)
			FeedParserWrapper.mostRecentItem = FeedItem.getMostRecent(cr);

		long itemDate = item.getDate();
		// itemDate >= subscription.lastItemUpdated
		if (FeedParserWrapper.mostRecentItem == null
				|| item.newerThan(FeedParserWrapper.mostRecentItem)) {

			if (itemDate > update_date) {
				update_date = itemDate;
			}
			addItem(subscription, item);
			add_num++;

			subscription.fail_count = 0;
			subscription.title = this.subscriptionTitle;
			subscription.description = this.subscriptionDescription;
			subscription.imageURL = this.subscriptionImage;
			subscription.lastItemUpdated = update_date;
			subscription.update(cr);
			log.debug("add url: " + subscription.url + "\n add num = "
					+ add_num);
		}
		return add_num;

	}

	private void addItem(Subscription subscription, FeedItem item) {
		Long sub_id = subscription.id;

		item.sub_id = sub_id;

		String where = ItemColumns.SUBS_ID + "=" + sub_id + " and "
				+ ItemColumns.RESOURCE + "= '" + item.resource + "'";

		Cursor cursor = cr.query(ItemColumns.URI,
				new String[] { BaseColumns._ID }, where, null, null);

		if (cursor.moveToFirst()) {
		} else {
			item.insert(cr);
		}

		if (cursor != null)
			cursor.close();

	}

}