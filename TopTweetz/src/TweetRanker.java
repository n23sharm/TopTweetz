import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 *	A tweet has two properties we care about:
 *	- It's own information (ID, and text)
 *	- A nested tweet object meaning this tweet has retweeted another one
 *	
 *	The ranker works by maintaining two data structures. The first one
 *	is a queue of the chunks received from the stream. Each chunk
 *	represents a duration in time (eg. 1 second of tweets). The ranker
 *	will keep (window / chunk duration) number of chunks around at any
 *	given time.
 *	
 *	The second data structure is a map which maps a tweet's ID to a
 *	consolidated tweet object. This map is what is used to determine the
 *	rank of any given tweet.
 *	
 *	Every time a chunk is received, we add it to the queue and update our
 *	consolidated mapping to take into account all the new data we recieved.
 *	If our queue has overflowed, we remove an item from the queue, and
 *	decrement all the data from our consolidated mapping so that data
 *	outside our current window is no longer considered.
 *	
 *	After each chunk, we do a sort on our consolidated list and print out
 *	the top 10 tweets we currently have.
 *	
 *	The consolidated mapping contains every tweet that has been referenced
 *	in the current window, either directly or as a retweet. We continuously
 *	update the retweet count of this consolidated object so that multiple
 *	references of the same tweet simply are stored once.
 *
 */
public class TweetRanker implements TweetChunkListener {

	private HashMap<Long, Tweet> mTweetRankMap;
	private Queue<ArrayList<Tweet>> mChunkQueue;

	private int mNumberOfChunks; 
	
	public TweetRanker(int numberOfChunks) {
		this.mNumberOfChunks = numberOfChunks;
		this.mTweetRankMap = new HashMap<Long, Tweet>();
		this.mChunkQueue = new LinkedList<ArrayList<Tweet>>();
	}
	
	@Override
	synchronized public void onTweetChunkReceived(ArrayList<Tweet> tweetChunk) {
		// Add the incoming tweets from the chunk to the mTweetRankMap 
		for (Tweet receivedTweet : tweetChunk) {
			Tweet tweet = receivedTweet.makeCopy();
			mTweetRankMap.put(tweet.getId(), tweet);
		
			// If the tweet is a retweet, and exists in the mTweetRankMap, increment its retweet count by 1.
			// If the retweet doesn't exist in the map, add it to the map with a retweet count of 0. We need 
			// to store the retweet count as 0 for a retweet not in our map since we don't want to consider 
			// any tweet actions that occurred outside of our time window. So even if the retweeted tweet had
			// 1000 retweets, we only want to consider how many retweets happend in the	last X minutes we care about.
			Tweet retweet = tweet.getRetweet();
			if (retweet != null) {
				if (mTweetRankMap.containsKey(retweet.getId())) {
					retweet.setRetweetCount(mTweetRankMap.get(retweet.getId()).getRetweetCount() + 1);
					mTweetRankMap.put(retweet.getId(), retweet);
				} else {
					retweet.setRetweetCount(0);
					mTweetRankMap.put(retweet.getId(), retweet);
				}
			}
		}
		
		mChunkQueue.add(tweetChunk);
		
		if (mChunkQueue.size() > mNumberOfChunks) {
			// remove the oldest chunk
			ArrayList<Tweet> oldestChunk = mChunkQueue.remove();
			
			for (Tweet oldTweet : oldestChunk) {
				Tweet tweet = oldTweet.makeCopy();
				if (mTweetRankMap.containsKey(tweet.getId())) {
					tweet.setRetweetCount(mTweetRankMap.get(tweet.getId()).getRetweetCount() - tweet.getRetweetCount());
					if (tweet.getRetweetCount() <= 0) {
						// clean up the tweets, to reduce memory usage
						mTweetRankMap.remove(tweet.getId());
					} else {
						tweet.setRetweetCount(mTweetRankMap.get(tweet.getId()).getRetweetCount() - tweet.getRetweetCount());
						mTweetRankMap.put(tweet.getId(), tweet);
					}
				} 
				
				Tweet retweet = tweet.getRetweet();
				if (retweet != null) {
					if (mTweetRankMap.containsKey(retweet.getId())) {
						retweet.setRetweetCount(mTweetRankMap.get(retweet.getId()).getRetweetCount() - 1);
						if (retweet.getRetweetCount() <= 0) {
							// clean up 
							mTweetRankMap.remove(retweet.getId());
						} else {
							mTweetRankMap.put(retweet.getId(), retweet);
						}
					}
				}
			}
		}
		
		printTopTenTweets();
	}
	
	private void printTopTenTweets() {
		List<Map.Entry<Long, Tweet>> list = new LinkedList<Map.Entry<Long, Tweet>>(mTweetRankMap.entrySet());
		 
		// Sort list based on comparator
		Collections.sort(list, new Comparator<Map.Entry<Long, Tweet>>() {
			public int compare(Map.Entry<Long, Tweet> o1, Map.Entry<Long, Tweet> o2) {
				Integer value1 = o1.getValue().getRetweetCount();
				Integer value2 = o2.getValue().getRetweetCount();
				return -1*value1.compareTo(value2);
			}
		});
		
		System.out.println("--------------------------------------------------------");
		System.out.println("Retweet Count \t | Tweet");
		System.out.println("--------------------------------------------------------");
		for (int i = 0; i < Math.min(10, list.size()); ++i) { 
			System.out.println(list.get(i).getValue().getRetweetCount() + " \t\t | " + list.get(i).getValue().getText());
		}
	}

}
