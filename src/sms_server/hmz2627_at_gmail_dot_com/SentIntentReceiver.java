package hmz2627_at_gmail_dot_com.sms_server;

import android.telephony.SmsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.app.Activity;

public class SentIntentReceiver extends BroadcastReceiver {
	
	Main actvt;
	
	SentIntentReceiver(final Main actvt) {
		this.actvt=actvt;
	}
	
	@Override
	public void onReceive(Context context, Intent arg1) {
		Logger.d("SentIntent: "+arg1.getIntExtra("requestCode", -100));
		Logger.d("SentIntent resultCode: "+getResultCode());
		switch (getResultCode()) {
		case Activity.RESULT_OK:
			Logger.d("SentIntentReceiver: OK");
			break;
		case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
			Logger.d("SentIntentReceiver: Generic failure");
			break;
		case SmsManager.RESULT_ERROR_NO_SERVICE:
			Logger.d("SentIntentReceiver: No service");
			break;
		case SmsManager.RESULT_ERROR_NULL_PDU:
			Logger.d("SentIntentReceiver: Null PDU");
			break;
		case SmsManager.RESULT_ERROR_RADIO_OFF:
			Logger.d("SentIntentReceiver: Radio off");
			break;
		}
		
		PendingIntentsInfo.storeResult("send", arg1.getIntExtra("requestCode", -100), getResultCode());

	}
}
