package hmz2627_at_gmail_dot_com.sms_server;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class IPs extends Activity {
	
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ips);
    }
	
	@Override
    public void onResume() {
        super.onResume();
		Current.currentActivity=this;
		String out=IfaceIPs.getAll();
		Logger.d("******************************");
		Logger.d(out);
		Logger.d("******************************");
		TextView ips_list = (TextView) findViewById(R.id.ips_list);
		if(!out.equals("")) ips_list.setText(out);
		else ips_list.setText(getResources().getString(R.string.no_ifaces));
	}

}
