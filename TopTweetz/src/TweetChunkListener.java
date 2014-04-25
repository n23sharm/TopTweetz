import java.util.ArrayList;

public interface TweetChunkListener {
	void onTweetChunkReceived(ArrayList<Tweet> tweetChunk);
}
