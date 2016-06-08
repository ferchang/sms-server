package hmz2627_at_gmail_dot_com.sms_server;

import android.telephony.SmsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.app.Activity;

public class DeliverIntentReceiver extends BroadcastReceiver {
	
	Main actvt;
	
	DeliverIntentReceiver(final Main actvt) {
		this.actvt=actvt;
	}
	
	@Override
	public void onReceive(Context context, Intent arg1) {
		Logger.d("DeliverIntent: "+arg1.getIntExtra("requestCode", -100));
		switch (getResultCode()) {
		case Activity.RESULT_OK:
			Logger.d("DeliverIntentReceiver: OK");
			break;
		case Activity.RESULT_CANCELED:
			Logger.d("DeliverIntentReceiver: Cancelled");
			break;
		}
		
		PendingIntentsInfo.storeResult("deliver", arg1.getIntExtra("requestCode", -100), getResultCode());

	}
	
}
