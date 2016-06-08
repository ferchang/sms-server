package hmz2627_at_gmail_dot_com.sms_server;

import android.net.wifi.WifiManager;
import java.nio.ByteOrder;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import android.content.Context;

import	java.net.*;
import java.util.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IfaceIPs {
	
	public static String getAll() {
		
		String out="";
		
		try{
		
		Enumeration<NetworkInterface> theIntfList = NetworkInterface.getNetworkInterfaces();
		List<InterfaceAddress> theAddrList = null;
		NetworkInterface theIntf = null;
		InetAddress theAddr = null;
		
		while(theIntfList.hasMoreElements()) {
			theIntf = theIntfList.nextElement();
			
			if(!theIntf.isUp()) continue;
			if(theIntf.isLoopback()) continue;

			Logger.d("--------------------");
			Logger.d(" " + theIntf.getDisplayName());
			String name=theIntf.getName();
			Logger.d("          name: " + name);
			
			if(name.startsWith("rmnet")) name="Data";
			else if(name.startsWith("rndis")) name="USB";
			else if(name.startsWith("bnep")) name="Bluetooth";
			else if(name.startsWith("wlan")) name="WiFi";
			
			Logger.d("     loopback?: " + theIntf.isLoopback());
			Logger.d("          ptp?: " + theIntf.isPointToPoint());
			Logger.d("      virtual?: " + theIntf.isVirtual());
			Logger.d("           up?: " + theIntf.isUp());

			theAddrList = theIntf.getInterfaceAddresses();
			Logger.d("     int addrs: " + theAddrList.size() + " total.");
			
			String ipPattern = "[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}";
			Pattern r = Pattern.compile(ipPattern);
			
			int addrindex = 0;
			for(InterfaceAddress intAddr : theAddrList)
			{
			   addrindex++;
			   theAddr = intAddr.getAddress();
			   if(theAddr.getClass().getSimpleName().equals("Inet6Address")) continue;
			   Logger.d("            " + addrindex + ").");
			   String tmp=theAddr.toString();
			   Logger.d("              ip: " + tmp);
			   Matcher m = r.matcher(tmp);
			   m.find();
			   out+=name+": "+m.group(0)+"\n\n";
			}
		 }
		}
		catch (SocketException e) { e.printStackTrace(); }
		
		return out;
		
		}

}
