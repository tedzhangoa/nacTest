import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import oa.as.*;

class NacTest 
{
	//Current Device specific settings
	private static final String CURR_NAC_IP = "10.0.40.83";
	
	private static final String CURR_NAC_LOC_ID = "040";
	private static final String CURR_NAC_DEV_INDEX = "083";

	//private static final String CURR_NAC_LOC_NAME = "";
	private static final String CURR_NAC_DEV_NAME = "OANC01";
	private static final String CURR_NAC_DIC_VER = "2";
	
	//Current Software 
	private static final String CURR_SW_REV = "22900";
	
	//Number of soucres and sinks
	private static final int CURR_DEV_SRC_COUNT = 5;
	private static final int CURR_DEV_SNK_COUNT = 8;
	
	private static final double DEF_GAIN = -80.00;
	
	@Test
	void test() 
	{
		//load DLLs
		System.loadLibrary("FTPlib");
		System.loadLibrary("netspireSDK");
		
		//Establish connection to CXS
		
		StringArray address = new StringArray();	//serverAddresses parameter
		
		address.add("10.1.40.100");
		
		KeyValueMap connecParam = new KeyValueMap();	//config parameter 
		
		connecParam.set("NETSPIRE_SDK_SOCKET_PORT", "20002"); 
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
		DeviceModel m = new DeviceModel(); 
		
		int timeout = 0; 
		
		while(true) 
		{
			devices = as.getDeviceStates();
			//System.out.println("Number of connected devices is " + devices.size());
			if (devices.size() > 0)// && !devices.get(0).getDeviceModel().getName().equals("Unknown")) 
			{
				for(int i = 0; i < devices.size();i++) //find our device by IP
				{
					d = devices.get(i);
					
					//System.out.println(m.getName());
					//System.out.println("Name: " + d.getName() + " State: " + d.getState());
					if (d.getIP().equals(CURR_NAC_IP)) 
					{
						m = d.getDeviceModel(); 
						break;
					}
					
				}
				break;
			}
			else if (timeout == 30){
				break;
			}
			else {
				timeout++;
			}

			try { Thread.sleep(3000); }
			catch (InterruptedException e) { }
		}
		 

		System.out.println("Checking device attributes");
		assertEquals(CURR_NAC_LOC_ID,d.getLocationId()); 
		assertEquals(CURR_NAC_DEV_INDEX, d.getDeviceIndex());
		
		String dest = Integer.toString(d.getDstNo());
		
		assertEquals("2"+ CURR_NAC_LOC_ID + CURR_NAC_DEV_INDEX, Integer.toString(d.getDstNo()));

		//assertEquals( ,d.getLocationName());
		assertEquals(CURR_NAC_DEV_NAME, d.getName());
		
		assertEquals(CURR_NAC_DIC_VER, Long.toString(d.getDictionaryVersion()));
		assertEquals(CURR_SW_REV, d.getSoftwareRevision());
		
		
		//Check Device model class 
		System.out.println("Checking device input and output");			
		
		System.out.println("NAC model name is: " + m.getName());
		System.out.println("NAC digital inputs: " + m.getDigitalInputCount());
		System.out.println("NAC digital outputs: " + m.getDigitalOutputCount());
		System.out.println("NAC audio outputs: " + m.getAudioOutputChannels());
		
			
		
		//PA Controller 
		PAController pac = new PAController(); 
		PaSourceArray paSourceList = new PaSourceArray();
		PaSource paSource = new PaSource();
		
		PaSinkArray paSinkList = new PaSinkArray();
		PaSink paSink = new PaSink();
		
		pac = as.getPAController();
		paSourceList = pac.getPaSources();
		paSinkList = pac.getPaSinks();
		
		//check local device sources and sinks
		int sourceCount = 0;
		int sinkCount = 0;
		
		//for gain testing
		Gain sinkGain = new Gain(); 
		sinkGain.setLevel(DEF_GAIN);
		
		
		
		//Show all sources
		for(int i=0; i < paSourceList.size(); i++) 
		{
			paSource = paSourceList.get(i);
			if(Integer.toString(paSource.getId()).contains(dest)) 
			{
				System.out.println("PA Source ID is: " + paSource.getId());
				sourceCount++;
			}
		}
		
		assertEquals(sourceCount, CURR_DEV_SRC_COUNT);
		
		
		for (int i=0; i < paSinkList.size(); i++) 
		{
			paSink = paSinkList.get(i);
			if(Integer.toString(paSink.getId()).contains(dest)) 
			{
				System.out.println("PA Sink ID is: " + paSink.getId());
				assertEquals(CURR_NAC_IP, paSink.getIpAddress()); //check IP of each sink against device IP
				sinkCount++;
				
				//System.out.println("gain level is: " + paSink.getGain().getLevel());
				//Test setting gain 
				paSink.setOutputGain(sinkGain);;
				assertEquals(paSink.getGain().getLevel(), DEF_GAIN);
				
			}
		}
		
		
		assertEquals(sinkCount, CURR_DEV_SNK_COUNT);
		
		
		as.disconnect(); 
		//fail("Not yet completed");
	}

}
