import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class TopTweetz {
	
	private static int CHUNK_DURATION_IN_SECONDS = 30;
	
	public static void main(String[] args) {
		String minutes = "";
		try {
			while (!isValidInput(minutes)) {
				System.out.println("Please enter the duration of the rolling window of time (in minutes).");
				BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
				minutes = bufferRead.readLine();
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	  
		final int numberOfChunks = (Integer.valueOf(minutes)*60)/CHUNK_DURATION_IN_SECONDS;
		Thread streamThread = new Thread() {
			public void run() {
				TwitterStream twitterStream = new TwitterStream();
				twitterStream.startConnection(new TweetRanker(numberOfChunks));
			}
		};
		streamThread.start();
		
	}
	
	private static boolean isValidInput(String minutes) {
		try {
			Integer.parseInt(minutes);
			return true;
		} catch (NumberFormatException nfe) {}
		return false;
	}
}
