package hmz2627_at_gmail_dot_com.sms_server;

import android.app.Activity;
import android.telephony.SmsManager;
import java.util.ArrayList;

public class PendingIntentsInfo {
	
	public static HttpResponder httpResponder;

	public static int requestCode=-1;
	
	public static ArrayList<int[]> sentResults=new ArrayList<int[]>();
	
	public static ArrayList<int[]> deliverResults=new ArrayList<int[]>();
	
	public static void storeResult(String kind, int requestCode, int resultCode) {
		int[] entry={requestCode, resultCode};
		if(kind.equals("send")) sentResults.add(entry);
		else deliverResults.add(entry);
	}
	
	public static String getResults(int req_code) {
		String json="{";
		boolean foundFlag=false;
		for(int i=0; i<sentResults.size(); i++) {
			if((sentResults.get(i)[0])==req_code) {
				json+="\"send\":\""+resultCodeToString(sentResults.get(i)[1])+"\"";
				foundFlag=true;
				break;
			}
		}
		if(!foundFlag) json+="\"send\":null";
		json+=",";
		foundFlag=false;
		for(int i=0; i<deliverResults.size(); i++) {
			if((deliverResults.get(i)[0])==req_code+1) {
				json+="\"deliver\":\""+resultCodeToString(deliverResults.get(i)[1])+"\"";
				foundFlag=true;
				break;
			}
		}
		if(!foundFlag) json+="\"deliver\":null";
		json+="}";
		Logger.d(json);
		
		return json;
	}
	
	public static String resultCodeToString(int code) {
		
		switch(code) {
			case Activity.RESULT_OK:
				return httpResponder.getString(R.string.RESULT_OK);
			case Activity.RESULT_CANCELED:
				return httpResponder.getString(R.string.RESULT_CANCELED);
			case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
				return httpResponder.getString(R.string.RESULT_ERROR_GENERIC_FAILURE);
			case SmsManager.RESULT_ERROR_NO_SERVICE:
				return httpResponder.getString(R.string.RESULT_ERROR_NO_SERVICE);
			case SmsManager.RESULT_ERROR_NULL_PDU:
				return httpResponder.getString(R.string.RESULT_ERROR_NULL_PDU);
			case SmsManager.RESULT_ERROR_RADIO_OFF:
				return httpResponder.getString(R.string.RESULT_ERROR_RADIO_OFF);
		}
		
		return "resultCodeToString: Unknown code! ("+code+")";
		
	}
	
}
