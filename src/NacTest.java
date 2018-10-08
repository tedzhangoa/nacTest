import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import oa.as.*;

class NacTest {
	
	@Test
	void test() {
		//load DLLs
		System.loadLibrary("FTPlib");
		System.loadLibrary("netspireSDK");
		
		//Establish connection to CXS
		
		StringArray address = new StringArray();	//serverAddresses parameter
		
		address.add("10.1.40.102");
		
		
		KeyValueMap connecParam = new KeyValueMap();	//config parameter 
		
		connecParam.set("NETSPIRE_SDK_SOCKET_PORT", "20000"); 
		connecParam.set("NETSPIRE_SDK_SET_LOG_LEVEL", "0");
		
		AudioServer as = new AudioServer();
		
		//attempt connection
		as.connect(address, connecParam);
		
		//test connection 
		while (true) 
		{
			if(as.isAudioConnected()) 
			{
				System.out.println("Connection established");
				break;
			}
		}
		
		
		DeviceStateArray devices;
		
		while(true) 
		{
			devices = as.getDeviceStates();
			
			System.out.println("Number of connected devices is " + devices.size());
	
			if (devices.size() > 0) 
			{
				break;
			}

			try { Thread.sleep(10000); }
			catch (InterruptedException e) { }
		}
		
		
		for(int i = 0; i < devices.size();i++) 
		{
			Device d = devices.get(i);
			System.out.println("Name: " + d.getName() + " State: " + d.getState());
		}
		
		fail("Not yet completed");
	}

}
