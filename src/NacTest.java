import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import oa.as.*;
import oa.as.PaSink.AnnouncementType;
import oa.as.PaSink.MuteState;
import oa.as.PaSink.State;
import oa.as.PaSource.AttachMode;
import oa.as.PaSource.AttachState;

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
	
	//Model specific settings
	private static final String CONAC02_MODEL = "CONAC02/4000:UFM";
	private static final int CONAC02_DIN = 8;
	private static final int CONAC02_DOUT = 8;
	private static final int CONAC02_AOUT = 4;
	
	//Number of sources and sinks
	//private static final int CURR_DEV_SRC_COUNT = 5;
	private static final int CURR_DEV_SNK_COUNT = 8;
	
	//Default gain
	private static final double DEF_GAIN = -60.00;
	
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
		
		connecParam.set("NETSPIRE_SDK_SOCKET_PORT", "20000"); 
		//connecParam.set("NETSPIRE_SDK_SET_LOG_LEVEL", "0");
		
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
		//=========================================================================
        // connection to audio server is established. idle forever and get devices
        //=========================================================================
		
		DeviceStateArray devices = new DeviceStateArray();
		Device c = new Device(); //CXS
		Device d = new Device(); //test device
		DeviceModel m = new DeviceModel(); 
		
		int timeout = 0; 
		
		while(true) 
		{
			devices = as.getDeviceStates();
			if (devices.size() > 0)
			{
				for(int i = 0; i < devices.size();i++) //find our device by IP
				{
					d = devices.get(i);

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
			
		
		//=====================================
        // Test basic device set-up attributes
        //=====================================
		
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
		assertEquals(c.getSoftwareRevision(), d.getSoftwareRevision()); //assuming auto-updated
		
		
	//Check Device Model class 
		assertEquals(CONAC02_MODEL, m.getName());
		assertEquals(CONAC02_DIN, m.getDigitalInputCount());
		assertEquals(CONAC02_DOUT, m.getDigitalOutputCount());
		assertEquals(CONAC02_AOUT, m.getAudioOutputChannels());
		
		System.out.println("Device attributes okay");
		
		//===========================
        // Test Digital input/output 
        //===========================
		
		System.out.println("Checking device digital input/output");
		//Digital inputs
		for(int i=1; i<= m.getDigitalInputCount(); i++) 
		{
			assertEquals(false, d.getInputState(i)); //should be un-set by default 
		}
		
		//Set digital outputs
		for(int i=1; i<=m.getDigitalOutputCount(); i++ ) 
		{
			assertEquals(false, d.getOutputState(i));
			d.setOutputState(i, 1);
			try { Thread.sleep(300); }
			catch (InterruptedException e) { }
		}
		//refresh device states 
		try { Thread.sleep(2000); }
		catch (InterruptedException e) { }
		d = refreshDevice(as); 

		//Un-set digital outputs
		for(int i=1; i<=m.getDigitalOutputCount(); i++ ) 
		{
			assertEquals(true, d.getOutputState(i)); //confirm set successfully
			d.setOutputState(i, 2);
			try { Thread.sleep(300); }
			catch (InterruptedException e) { }
		}
		
		//refresh device states
		try { Thread.sleep(2000); }
		catch (InterruptedException e) { }
		d = refreshDevice(as);
		
		System.out.println("Digital input/output okay");
		
		//=====================================
        // Test PA System - Sources and Sinks
        //=====================================
		PAController pac = new PAController(); 
		
		PaSourceArray paSourceList = new PaSourceArray();
		PaSource paSource = new PaSource();
		
		PaSinkArray paSinkList = new PaSinkArray();
		PaSink paSink = new PaSink();
		
		pac = as.getPAController();
		
		//counters checking local device sources and sinks
		int sourceCount = 0;
		int sinkCount = 0;
		
		//for gain testing
		Gain sinkGain = new Gain(); 
		sinkGain.setLevel(DEF_GAIN);
		
		System.out.println("Testing audio sinks and sources");
		/********************************************************************************
		//Sources 
		for(int a=0; a<=1; a++) //2 iterations
		{
			paSourceList = pac.getPaSources(); //refresh source list to get new gains
			
			for(int i=0; i < paSourceList.size(); i++) 
			{
				paSource = paSourceList.get(i);
				if(Integer.toString(paSource.getId()).contains(dest)) //partial matching device sources
				{
					assertEquals(CURR_NAC_IP, paSource.getIpAddress());
					if(paSource.getType().name().equals("NETSPIRE_ANALOG_INPUT")) //only count analog outs 
					{
						if (a==0) //first iteration, set gains
						{
							sourceCount++;
							paSource.setGain(DEF_GAIN, true, false);
		
							try { Thread.sleep(5000); }
							catch (InterruptedException e) { }		
						}
						else {
							assertEquals(DEF_GAIN, paSource.getGain()); //second iteration, check gains
						}
					}
				}
			}
			if (a==0) 
			{
				try { Thread.sleep(15000); }  //let gain changes take place
				catch (InterruptedException e) { }	
			}
		}
		assertEquals(sourceCount, CONAC02_AOUT);
		
		System.out.println("Audio sources okay");
		
		//Sinks
		for (int a=0; a<=1; a++) 
		{
			paSinkList = pac.getPaSinks(); //refresh sink list to get new gains
			for (int i=0; i < paSinkList.size(); i++) 
			{
				paSink = paSinkList.get(i);
				if(Integer.toString(paSink.getId()).contains(dest)) //partial matching device sinks
				{
					assertEquals(CURR_NAC_IP, paSink.getIpAddress()); //check IP of each sink against device IP
					if (a==0)  //first iteration, set gains
					{
						sinkCount++;
						paSink.setGain(sinkGain, true, false);
						
						try { Thread.sleep(5000); }
						catch (InterruptedException e) { }
					}
					else 
					{
						assertEquals(DEF_GAIN, paSink.getGain().getLevel()); //second iteration, check gains
					}
				}
			}
			if (a==0) 
			{
				try { Thread.sleep(15000); }  //let gain changes take place
				catch (InterruptedException e) { }	
			}
		}
		assertEquals(sinkCount, CURR_DEV_SNK_COUNT);
		System.out.println("Audio sinks okay");
		********************************************************************************/	
		
		//=========================================
        // Test PA System - Announcement playback
        //=========================================
		
		//setup test announcement for playback on sinks
		
		NumberArray dvaItems = new NumberArray();
		dvaItems.clear();
		dvaItems.add(99001);
		dvaItems.add(99200);
		
		StringArray outputZoneList = new StringArray();
		StringArray outputVisualList = new StringArray();
		Gain gain = new Gain();
		
		//Testing zones
		PaZoneArray paZoneList = new PaZoneArray();
		PaZone zone = new PaZone(); 
		PaZone allZone = new PaZone();
		
		paZoneList = pac.getPaZones();
		
		System.out.println("Testing PA single zone play back");
		
		//Testing playback from one zone at a time
		for (int i=0; i < paZoneList.size(); i++) 
		{
			zone = paZoneList.get(i);
			
			if (zone.getId().contains("Test/Test"))
			{
				outputZoneList.clear(); //use one test zone at a time
				outputZoneList.add(zone.getId());
				pac.playMessage(outputZoneList, outputVisualList, gain, dvaItems, null, false, false, 0, 0, 0); //play to zone
				
				try { Thread.sleep(1500); }
				catch (InterruptedException e) { }
				
				if(Character.isDigit(zone.getId().charAt(9))) //for single sink zones
				{
					
					paSinkList = zone.getMembers();
					assertEquals(1, paSinkList.size()); 
					
					paSink = paSinkList.get(0);
					
					//check message is being played
					assertEquals(AnnouncementType.FILE_PLAY, paSink.getAnnouncementType());
					assertEquals(State.ACTIVE, paSink.getState());
		
					try { Thread.sleep(8000); }
					catch (InterruptedException e) { }
				}
			}
			
			if (zone.getId().equals("Test/TestAll"))
			{
				allZone = zone; //save zone for all
			}
		}
		
		//Test playback queuing multiple messages
		/*
		System.out.println("Testing PA queued message play back");
		
		outputZoneList.clear();
		outputZoneList.add("Test/Test1");
		outputZoneList.add("Test/Test2");
		
		pac.playMessage(outputZoneList, outputVisualList, gain, dvaItems, null, false, false, 0, 0, 0);
		pac.playMessage(outputZoneList, outputVisualList, gain, dvaItems, null, false, false, 0, 0, 0);
		pac.playMessage(outputZoneList, outputVisualList, gain, dvaItems, null, false, false, 0, 0, 0);
		pac.playMessage(outputZoneList, outputVisualList, gain, dvaItems, null, false, false, 0, 0, 0);
		
		try { Thread.sleep(30000); }
		catch (InterruptedException e) { }
		*/
		
		//Test playback using software trigger from an analog source
		
		System.out.println("Testing live PA software trigger play back");
		
		int sourceId = Integer.parseInt(Integer.toString(d.getDstNo()) + "01"); //use the first source
		
		pac.attachPaSource(sourceId); //attach
		
		paSourceList = pac.getPaSources();
		
		for(int i=0; i<paSourceList.size(); i++) //retrieve the first PA source 
		{
			paSource = paSourceList.get(i);
			if (paSource.getId() == sourceId) 
			{
				break;
			}
		}
		
		assertEquals(AttachState.ATTACHED, paSource.getAttachState());
		
		paSource.attachPaZone("Test/TestAll", AttachMode.ADD_TO_EXISTING_SET); //attach zone with all sinks to source
		
		int swTrigId = pac.createSwPaTrigger(sourceId, 100);
		pac.activateSwPaTrigger(swTrigId);

		try { Thread.sleep(10000); }
		catch (InterruptedException e) { }
		
		paSinkList = allZone.getMembers();
		
		for (int i=0; i< paSinkList.size(); i++) { //ensure all sinks are active 
			paSink = paSinkList.get(i);
			assertEquals(AnnouncementType.VOIP_STREAMING, paSink.getAnnouncementType());
			assertEquals(State.ACTIVE, paSink.getState());
		}
		pac.deactivateSwPaTrigger(swTrigId);
		pac.detachPaSource(sourceId);
		
		
		
		
		System.out.println("All PA tets okay");
		
		
		
		System.out.println("All tests okay");
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
	
	
	

	
	/*
	 * Check audio zone/sink activity and announcement type during playback  
	 * 
	 * Muting/unmuting PA sinks //SDK implementation incomplete
	 * Muting/unmuting PA zones // '' 
	 * 
	 * Test attaching/detaching sources
	 * Test announcement with analog input 
	 * 
	 * Playback using messages with different priorities
	 * 
	 * Hardware triggers for source
	 */
	
}
