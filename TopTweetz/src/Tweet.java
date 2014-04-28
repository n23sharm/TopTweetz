
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
	
	public Tweet makeCopy() {
		Tweet retweet = this.getRetweet() == null ? null : this.getRetweet().makeCopy();
		return new Tweet(this.getId(), this.getText(), this.getRetweetCount(), retweet);
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
