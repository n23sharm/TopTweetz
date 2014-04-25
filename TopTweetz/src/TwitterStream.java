import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONException;
import org.json.JSONObject;

public class TwitterStream {
	
	private static final String STREAM_URL = "https://stream.twitter.com/1.1/statuses/filter.json";
	
	private static final String GET_METHOD = "GET";
	private static final String OAUTH_PREFIX = "oauth";
	
	private static final String TOKEN = "451681582-U1WtEumAli00bqZxFY8uoeJfMXiTGUR08UeUc82W";
	private static final String TOKEN_SECRET = "iHu0lwiovVmLmzfc4FuL1GWq2TyXsg9r3YwLk7A4V6x9M";
	private static final String CONSUMER_KEY = "yP805mjDiURC1Zy0uwEEm7L7E";
	private static final String CONSUMER_SECRET = "eiWc6Xh3k9atjYkzQqDcEXeQawP7ad9aVRmwfDOE2JCHB6XSl1";
	
	private static final String PARAM_LOCATIONS = "locations";
	private static final String PARAM_LOCATIONS_WHOLE_WORLD = "-180,-90,180,90";
	
	private static final int CHUNK_DURATION_IN_SECONDS = 1;
	
	public void startConnection(TweetChunkListener listener) {
		while (true) {
			CloseableHttpClient httpClient = HttpClients.createDefault();
			
			HashMap<String, String> paramMap = new HashMap<String, String>();
			paramMap.put("oauth_token", TOKEN);
			paramMap.put("oauth_consumer_key", CONSUMER_KEY);
			paramMap.put("oauth_nonce", getAuthNonce());
			paramMap.put("oauth_signature_method", "HMAC-SHA1");
			paramMap.put("oauth_timestamp", String.valueOf(System.currentTimeMillis()/1000));
			paramMap.put("oauth_version", "1.0");
			paramMap.put(PARAM_LOCATIONS, PARAM_LOCATIONS_WHOLE_WORLD);
			
			StringBuilder authorizationHeader = new StringBuilder("OAuth ");
			for (String key : paramMap.keySet()) {
				if (key.startsWith(OAUTH_PREFIX)) {
					// append the percent encoded key
					authorizationHeader.append(percentEncode(key));
					authorizationHeader.append("=\"");
					authorizationHeader.append(percentEncode(paramMap.get(key)));
					authorizationHeader.append("\", ");
				}
			}
			authorizationHeader.append(percentEncode("oauth_signature")).append("=\"").append(percentEncode(getSignatureKey(paramMap)));
			
			StringBuilder url = new StringBuilder();
			try {
				url.append(STREAM_URL).append("?").append(PARAM_LOCATIONS).append("=").append(URLEncoder.encode(PARAM_LOCATIONS_WHOLE_WORLD, "UTF-8"));
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}
			
			HttpGet httpGet = new HttpGet(url.toString());
			httpGet.setHeader("Authorization", authorizationHeader.toString());
			
			CloseableHttpResponse response;
			try {
				response = httpClient.execute(httpGet);
	
	            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
	            
	            ArrayList<Tweet> tweetChunk;
	            String line;
	            long lastTime = System.currentTimeMillis()/1000;
	            
	            tweetChunk = new ArrayList<Tweet>();
	            while ((line = reader.readLine()) != null) {
	            	Tweet tweet = parseTweet(line);
	            	if (tweet != null) {
	            		tweetChunk.add(tweet);
	            	}
	            	if (System.currentTimeMillis()/1000 - lastTime > CHUNK_DURATION_IN_SECONDS &&
	            			tweetChunk.size() >= 10) {
	            		lastTime = System.currentTimeMillis()/1000;
	            		listener.onTweetChunkReceived(tweetChunk);
	            		tweetChunk = new ArrayList<Tweet>();
	            	}
	            }
	
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				// Connection can close on us. We will loop again and
				// open a new one
			}
		}
	}
	
	private Tweet parseTweet(String data) {
		try {			
			JSONObject jsonObject = new JSONObject(data);
			long id = jsonObject.getLong("id");
			String text = jsonObject.getString("text");
			int retweetCount = jsonObject.getInt("retweet_count");
			
			// Check if this is a retweet
			JSONObject retweetJSON = jsonObject.optJSONObject("retweeted_status");
			Tweet retweet = null;
			if (retweetJSON != null) {
				long retweetId = retweetJSON.getLong("id");
				String retweetText = retweetJSON.getString("text");
				int retweetRetweetCount = retweetJSON.getInt("retweet_count");
				
				retweet = new Tweet(retweetId, retweetText, retweetRetweetCount, null);
			}
		
			return new Tweet(id, text, retweetCount, retweet);
		} catch (JSONException e) {
			return null;
		}
	}
	
	private static String getSignatureKey(HashMap<String, String> paramMap) {
		ArrayList<String> alphabeticalKeys = new ArrayList<String>();
		alphabeticalKeys.addAll(paramMap.keySet());
		Collections.sort(alphabeticalKeys);
		
		StringBuilder signatureParams = new StringBuilder();
		for (String key : alphabeticalKeys) {
			signatureParams.append(percentEncode(key)).append("=").append(percentEncode(paramMap.get(key))).append("&");
		}
		
		String signatureParamsString = signatureParams.substring(0, signatureParams.length()-1);
		signatureParamsString = percentEncode(signatureParamsString);
		
		StringBuilder signature = new StringBuilder();
		signature.append(GET_METHOD).append("&").append(percentEncode(STREAM_URL)).append("&").append(signatureParamsString);
		
		StringBuilder signingKey = new StringBuilder();
		signingKey.append(percentEncode(CONSUMER_SECRET)).append("&").append(percentEncode(TOKEN_SECRET));
		
		String result = "";
		try {
			SecretKeySpec keySpec = new SecretKeySpec(signingKey.toString().getBytes(), "HmacSHA1");
			Mac mac = Mac.getInstance("HmacSHA1");
			mac.init(keySpec);
			byte[] resultBytes = mac.doFinal(signature.toString().getBytes());
			result = Base64.encodeBase64String(resultBytes);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		}

		return result;
	}
	
	private static String percentEncode(String s) {
		if (s == null) {
		    return "";
		}
		try {
		    return URLEncoder.encode(s, "UTF-8").replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
		} catch (UnsupportedEncodingException wow) {
		    throw new RuntimeException(wow.getMessage(), wow);
		}
	}
	 
	 private static String getAuthNonce() {
	    return Base64.encodeBase64String(UUID.randomUUID().toString().replace("-", "").getBytes());
	 }
}