import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

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
		// Process the incoming chunk of tweets and store each tweet in a global
		// hashmap, keeping track of its retweet count that occurred in the window 
		// we cared about. For example, if a tweet was retweeted 5 times before this
		// window began, and was retweeted again during this window, it will only mark
		// it as being retweeted once
		for (Tweet tweet : tweetChunk) {
			mTweetRankMap.put(tweet.getId(), tweet);
		
			// check if it's retweet exists too
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
			
			for (Tweet tweet : oldestChunk) {
				long id = tweet.getId();
				if (mTweetRankMap.containsKey(id)) {
					tweet.setRetweetCount(mTweetRankMap.get(id).getRetweetCount() - tweet.getRetweetCount());
					if (tweet.getRetweetCount() <= 0) {
						// clean up the tweets, to reduce memory usage
						mTweetRankMap.remove(id);
					} else {
						tweet.setRetweetCount(mTweetRankMap.get(id).getRetweetCount() - tweet.getRetweetCount());
						mTweetRankMap.put(id, tweet);
					}
				} 
				
				Tweet retweet = tweet.getRetweet();
				if (retweet != null) {
					if (mTweetRankMap.containsKey(retweet.getId())) {
						retweet.setRetweetCount(mTweetRankMap.get(retweet.getId()).getRetweetCount() - 1);
						mTweetRankMap.put(retweet.getId(), retweet);
					}
				}
			}
		}
		
		printTopTenTweets();
	}
	
	private void printTopTenTweets() {
		List<Map.Entry<Long, Tweet>> list = new LinkedList<Map.Entry<Long, Tweet>>(mTweetRankMap.entrySet());
		 
		// sort list based on comparator
		Collections.sort(list, new Comparator<Map.Entry<Long, Tweet>>() {
			public int compare(Map.Entry<Long, Tweet> o1, Map.Entry<Long, Tweet> o2) {
				Integer value1 = o1.getValue().getRetweetCount();
				Integer value2 = o2.getValue().getRetweetCount();
				return value1.compareTo(value2);
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
