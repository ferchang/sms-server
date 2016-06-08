package hmz2627_at_gmail_dot_com.sms_server;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class About extends Activity {
	
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
    }
	
	@Override
    public void onResume() {
        super.onResume();
		Current.currentActivity=this;
	}

}
