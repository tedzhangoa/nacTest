import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import oa.as.*;

class NacTest {
	//Current Device specific settings
	private static final String CURR_NAC_IP = "10.0.40.83";
	private static final String CURR_NAC_DEV_INDEX = "083";
	//private static final String CURR_NAC_DST_NUM = ;
	private static final String CURR_NAC_LOC_ID = "040";
	//private static final String CURR_NAC_LOC_NAME = "";
	private static final String CURR_NAC_DEV_NAME = "NAC";
	private static final String CURR_NAC_DIC_VER = "13";
	
	
	//Current Software 
	private static final String CURR_SW_REV = "22903";
	
	
	
	
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
		
		
		DeviceStateArray devices = new DeviceStateArray();
		Device d = new Device(); 
		
		while(true) 
		{
			devices = as.getDeviceStates();
			//System.out.println("Number of connected devices is " + devices.size());
			if (devices.size() > 0) 
			{
				for(int i = 0; i < devices.size();i++) //find our device by IP
				{
					d = devices.get(i);
					//System.out.println("Name: " + d.getName() + " State: " + d.getState());
					if (d.getIP().equals(CURR_NAC_IP)) 
					{
						break;
					}
					
				}
				//device not connected
				break;
			}

			try { Thread.sleep(10000); }
			catch (InterruptedException e) { }
		}
		 

		System.out.println("Checking device attributes");
		assertEquals(CURR_NAC_DEV_INDEX, d.getDeviceIndex());
		assertEquals("2"+ CURR_NAC_LOC_ID + CURR_NAC_DEV_INDEX, Integer.toString(d.getDstNo()));
		assertEquals(CURR_NAC_LOC_ID,d.getLocationId()); 
		//assertEquals( ,d.getLocationName());
		assertEquals(CURR_NAC_DEV_NAME, d.getName());
		
		assertEquals(CURR_NAC_DIC_VER, Long.toString(d.getDictionaryVersion()));
		assertEquals(CURR_SW_REV, d.getSoftwareRevision());
		
		
		
		System.out.println("Checking device input and output states");			
		//assertEquals()
		
		
		
		//System.out.println("NAC software version is: " + d.getSoftwareRevision());
		//System.out.println("NAC location ID is: " + d.getLocationId());
		//System.out.println("NAC device index is: " + d.getDeviceIndex());
		
		as.disconnect(); 
		//fail("Not yet completed");
	}

}
