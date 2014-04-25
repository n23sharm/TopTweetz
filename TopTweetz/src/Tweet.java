
public class Tweet {
	private long mId;
	private String mText;
	private int mRetweetCount;
	private Tweet mRetweet;
	
	public Tweet(long id, String text, int retweetCount, Tweet retweet) {
		this.mId = id;
		this.mText = text;
		this.mRetweetCount = retweetCount;
		this.mRetweet = retweet;
	}

	public long getId() {
		return mId;
	}
	
	public String getText() {
		return mText;
	}

	public int getRetweetCount() {
		return mRetweetCount;
	}
	
	public Tweet getRetweet() {
		return mRetweet;
	}
	
	public void setRetweetCount(int count) {
		this.mRetweetCount = count;
	}
}
