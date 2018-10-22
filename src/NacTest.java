import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import oa.as.*;

class NacTest 
{
	//Current Device specific settings
	private static final String CURR_CXS_IP = "10.1.40.100";
	private static final String CURR_NAC_IP = "10.0.40.83";
	private static final String CURR_NAC_LOC_ID = "040";
	private static final String CURR_NAC_DEV_INDEX = "083";
	private static final String CURR_NAC_DEV_NAME = "OANC01";
	
	private static final String DEV_CLASS = "NETWORK_AUDIO_CONTROLLER";
	//private static final String CURR_NAC_LOC_NAME = "";
	
	
	//Number of sources and sinks
	private static final int CURR_DEV_SRC_COUNT = 5;
	private static final int CURR_DEV_SNK_COUNT = 8;
	
	//Default gain
	private static final double DEF_GAIN = -80.00;
	
	@Test
	void test() 
	{
		//load DLLs
		System.loadLibrary("FTPlib");
		System.loadLibrary("netspireSDK");
		
		//Establish connection to CXS
		
		StringArray address = new StringArray();	//serverAddresses parameter
		
		address.add(CURR_CXS_IP);
		
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
		Device c = new Device(); //CXS
		Device d = new Device(); //test device
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
					if (d.getIP().equals(CURR_CXS_IP)) //retrieve CXS
					{
						c = d;
					}
					
					if (d.getIP().equals(CURR_NAC_IP)) //retrieve test device
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
		 
		
		
	//Possible setup of device
		
		//1. test dictionary update
			//initiate update through CXS
			//check resultant dictionary version on device
		
		//2. test dictionary changes on single item
			
		
		//System.out.println("CXS dictionary version is: " + as.getDictionaryChangeset().isEmpty());
		
		
		
	//Check device attributes - pre-set attributes
		System.out.println("Checking device attributes");
		assertEquals(CURR_NAC_LOC_ID,d.getLocationId()); 
		assertEquals(CURR_NAC_DEV_INDEX, d.getDeviceIndex());
		assertEquals(CURR_NAC_DEV_NAME, d.getName());
		assertEquals(DEV_CLASS, d.getDeviceClass().name());
		
		String dest = Integer.toString(d.getDstNo());	
		assertEquals("2"+ CURR_NAC_LOC_ID + CURR_NAC_DEV_INDEX, Integer.toString(d.getDstNo()));
		
	//Check device attributes - against CXS
		assertEquals(c.getDictionaryVersion(), d.getDictionaryVersion());
		//assertEquals(c.getSoftwareRevision(), d.getSoftwareRevision());
		
		
	//Check Device Model class 
		
		System.out.println("Checking device input and output");			
		System.out.println("NAC model name is: " + m.getName());
		System.out.println("NAC digital inputs: " + m.getDigitalInputCount());
		System.out.println("NAC digital outputs: " + m.getDigitalOutputCount());
		System.out.println("NAC audio outputs: " + m.getAudioOutputChannels());
		
		
		
	//Check device Digital input/output setting/getting

		//Digital inputs
		for(int i=1; i<= m.getDigitalInputCount(); i++) {
			assertEquals(false, d.getInputState(i));
		}
		
		//Set digital outputs
		for(int i=1; i<=m.getDigitalOutputCount(); i++ ) {
			assertEquals(false, d.getOutputState(i));
			d.setOutputState(i, 1);
			try { Thread.sleep(500); }
			catch (InterruptedException e) { }
		}
		//refresh device states 
		d = refreshDevice(as); 

		//Un-set digital outputs
		for(int i=1; i<=m.getDigitalOutputCount(); i++ ) {
			assertEquals(true, d.getOutputState(i)); //confirm set successfully
			d.setOutputState(i, 2);
			try { Thread.sleep(500); }
			catch (InterruptedException e) { }
		}
		//refresh device states
		d = refreshDevice(as);
		
	//PA Controller testing
		PAController pac = new PAController(); 
		PaSourceArray paSourceList = new PaSourceArray();
		PaSource paSource = new PaSource();
		
		PaSinkArray paSinkList = new PaSinkArray();
		PaSink paSink = new PaSink();
		
		pac = as.getPAController();
		paSourceList = pac.getPaSources();
		paSinkList = pac.getPaSinks();
		
		//counters for checking local device sources and sinks
		int sourceCount = 0;
		int sinkCount = 0;
		
		//for gain testing
		Gain sinkGain = new Gain(); 
		sinkGain.setLevel(DEF_GAIN);
		
		//Show all sources
		for(int i=0; i < paSourceList.size(); i++) 
		{
			paSource = paSourceList.get(i);
			if(Integer.toString(paSource.getId()).contains(dest)) //partial matching device sources
			{
				System.out.println("PA Source ID is: " + paSource.getId());
				sourceCount++;
			}
		}
		assertEquals(sourceCount, CURR_DEV_SRC_COUNT);
		
		//Show all sinks
		for (int i=0; i < paSinkList.size(); i++) 
		{
			paSink = paSinkList.get(i);
			if(Integer.toString(paSink.getId()).contains(dest)) //partial matching device sinks
			{
				System.out.println("PA Sink ID is: " + paSink.getId());
				assertEquals(CURR_NAC_IP, paSink.getIpAddress()); //check IP of each sink against device IP
				sinkCount++;
				
				//System.out.println("gain level is: " + paSink.getGain().getLevel());
				//Test setting gain 
				paSink.setOutputGain(sinkGain);
				assertEquals(paSink.getGain().getLevel(), DEF_GAIN);
				
			}
		}
		assertEquals(sinkCount, CURR_DEV_SNK_COUNT);
		
		
	//Test PA on All sinks	
		
		//setup test announcement for playback on sinks
		
		int dictionaryId = 100000; //single dictionary item
		NumberArray dvaItems = new NumberArray();
		dvaItems.add(dictionaryId);
		
		StringArray outputZoneList = new StringArray();
		StringArray outputVisualList = new StringArray();
		
		
		//Testing zones
		PaZoneArray paZoneList = new PaZoneArray();
		PaZone zone = new PaZone(); 
		
		paZoneList = pac.getPaZones();
		for (int i=0; i < paZoneList.size(); i++) 
		{
			zone = paZoneList.get(i);
			if (zone.getId().contains("Test/Test"))
			{
				outputZoneList.add(zone.getId());
			}
		}
		
		//Play out to all relevant zones
		pac.playMessage(outputZoneList, outputVisualList, sinkGain, dvaItems, null, false, true, 0, 0, 0);
		
		
		
		as.disconnect(); 
	}

	
	
	
	
	private Device refreshDevice (AudioServer as) 
	{
		DeviceStateArray devices = new DeviceStateArray();
		Device d = new Device(); //test device
		
		while(true) 
		{
			devices = as.getDeviceStates();
			if (devices.size() > 0)// && !devices.get(0).getDeviceModel().getName().equals("Unknown")) 
			{
				for(int i = 0; i < devices.size();i++) //find our device by IP
				{
					d = devices.get(i);				
					if (d.getIP().equals(CURR_NAC_IP)) //retrieve test device
					{
						return d;
					}
				}
				break;
			}
			try { Thread.sleep(3000); }
			catch (InterruptedException e) { }
		}
		return null;
	}
	
	
	
}
