package org.bottiger.podcast.parser.syndication.handler;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import org.bottiger.podcast.parser.syndication.namespace.Namespace;
import org.bottiger.podcast.parser.syndication.namespace.SyndElement;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.Subscription;

/**
 * Contains all relevant information to describe the current state of a
 * SyndHandler.
 */
public class HandlerState {

	/** Feed that the Handler is currently processing. */
	protected ISubscription feed;
	protected ArrayList<FeedItem> items;
	protected FeedItem currentItem;
	protected Stack<SyndElement> tagstack;
	/** Namespaces that have been defined so far. */
	protected HashMap<String, Namespace> namespaces;
	protected Stack<Namespace> defaultNamespaces;
	/** Buffer for saving characters. */
	protected StringBuffer contentBuf;

	public HandlerState(@NonNull ISubscription feed) {
		this.feed = feed;
		items = new ArrayList<>();
		tagstack = new Stack<>();
		namespaces = new HashMap<>();
		defaultNamespaces = new Stack<>();
	}

	public ISubscription getSubscription() {
		return feed;
	}

    public void setSubscription(@NonNull ISubscription argSubscription) {
        feed = argSubscription;
    }

	public ArrayList<FeedItem> getItems() {
		return items;
	}

	public FeedItem getCurrentItem() {
		return currentItem;
	}

	public Stack<SyndElement> getTagstack() {
		return tagstack;
	}

	public void setFeed(Subscription feed) {
		this.feed = feed;
	}

	public void setCurrentItem(FeedItem currentItem) {
		this.currentItem = currentItem;
	}

	/**
	 * Returns the SyndElement that comes after the top element of the tagstack.
	 */
	public SyndElement getSecondTag() {
		SyndElement top = tagstack.pop();
		SyndElement second = tagstack.peek();
		tagstack.push(top);
		return second;
	}

	public SyndElement getThirdTag() {
		SyndElement top = tagstack.pop();
		SyndElement second = tagstack.pop();
		SyndElement third = tagstack.peek();
		tagstack.push(second);
		tagstack.push(top);
		return third;
	}

	public StringBuffer getContentBuf() {
		return contentBuf;
	}

}
