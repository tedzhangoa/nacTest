import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import oa.as.*;
import oa.as.PaSink.AnnouncementType;
import oa.as.PaSink.State;
import oa.as.PaSource.AttachMode;
import oa.as.PaSource.AttachState;

class NacTest 
{

	//============================
    // Device setup pre-requisites 
    //============================
	
	/*
	 *
	 * 1. IP address		
	 * 2. Device identification: location ID, device index, device name
	 * 
	 * 3. Audio zones: Test/Test1...Test/TestN for N local audio sinks
	 * 4. Audio zone: Test/TestAll with all local sinks routed
	 * 
	 * 5. Microphone plugged in audio Input 1 for activating live PA
	 * 
	 */
	 //============================
	
	//Current Device specific settings
	private static final String CURR_CXS_IP = "10.1.40.100";
	private static final String CURR_NAC_IP = "10.0.40.83";
	private static final String CURR_NAC_LOC_ID = "040";
	private static final String CURR_NAC_DEV_INDEX = "083";
	private static final String CURR_NAC_DEV_NAME = "NAC02_2";
	
	private static final String DEV_CLASS = "NETWORK_AUDIO_CONTROLLER";
	//private static final String CURR_NAC_LOC_NAME = "";
	
	//Model specific settings
	private static final String CONAC02_MODEL = "CONAC02/4000:UFM";
	private static final int CONAC02_DIN = 8;
	private static final int CONAC02_DOUT = 8;
	private static final int CONAC02_AOUT = 4;
	
	//Number of sources and sinks
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
		address.clear();
		
		address.add(CURR_CXS_IP);
		
		KeyValueMap connecParam = new KeyValueMap();	//config parameter 
		connecParam.clear();
		
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
			devices.clear();
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
		 

		//=====================================
        // Test basic device set-up attributes
        //=====================================
		
	//Check device attributes - pre-set attributes
		System.out.println();
		System.out.println("Testing device attributes");
		
		System.out.println("	Pre-set attributes");
		assertEquals(CURR_NAC_LOC_ID, d.getLocationId(), "Device Location ID incorrect"); 
		assertEquals(CURR_NAC_DEV_INDEX, d.getDeviceIndex(), "Device Index incorrect");
		assertEquals(CURR_NAC_DEV_NAME, d.getName(), "Device Name incorrect");
		
		
		String dest = Integer.toString(d.getDstNo());	
		assertEquals("2"+ CURR_NAC_LOC_ID + CURR_NAC_DEV_INDEX, dest, "Device Destination Number inconsistent ");
		
	//Check software and dictionary consistency against CXS
		System.out.println("	Dictionary and software consistency");
		assertEquals(c.getDictionaryVersion(), d.getDictionaryVersion(), "Device Dictionary Version inconsistent");
		assertEquals(c.getSoftwareRevision(), d.getSoftwareRevision(), "Device Software Revision inconsistent"); //assuming auto-updated
		
		System.out.println("	Device model and class consistency");
		assertEquals(DEV_CLASS, d.getDeviceClass().name(), "Device Class inconsistent");
	//Check Device Model class 
		assertEquals(CONAC02_MODEL, m.getName(), "Device Model Name inconsistent");
		assertEquals(CONAC02_DIN, m.getDigitalInputCount(), "Digital Input count inconsistent");
		assertEquals(CONAC02_DOUT, m.getDigitalOutputCount(), "Digitial Output count inconsistent");
		assertEquals(CONAC02_AOUT, m.getAudioOutputChannels(), "Audio Output count inconsistent");
		
		System.out.println("Device attributes pass");
		System.out.println();
		
		//===========================
        // Test Digital input/output 
        //===========================
		
		System.out.println("Testing device digital input/output");
		//Digital inputs
		System.out.println("	Digital input default state");
		for(int i=1; i<= m.getDigitalInputCount(); i++) 
		{
			assertFalse(d.getInputState(i), "Digital Input number " + i +" default state not false"); //should be un-set by default 
		}
		
		System.out.println("	Digital output default state");
		//Set digital outputs
		for(int i=1; i<=m.getDigitalOutputCount(); i++ ) 
		{
			assertFalse(d.getOutputState(i), "Digital Output number "+ i + " default state not false");
			d.setOutputState(i, 1);
			try { Thread.sleep(300); }
			catch (InterruptedException e) { }
		}
		//refresh device states 
		try { Thread.sleep(2000); }
		catch (InterruptedException e) { }
		d = refreshDevice(as); 

		System.out.println("	Digital output set state");
		//Check and then un-set digital outputs
		for(int i=1; i<=m.getDigitalOutputCount(); i++ ) 
		{
			assertTrue(d.getOutputState(i), "Digital Output number " + i + " not set to true"); //confirm set successfully
			d.setOutputState(i, 2);
			try { Thread.sleep(300); }
			catch (InterruptedException e) { }
		}
		
		System.out.println("	Digital output reset state");
		//refresh device states
		try { Thread.sleep(2000); }
		catch (InterruptedException e) { }
		d = refreshDevice(as);
		
		System.out.println("Digital input/output pass");
		System.out.println();
		
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
		///********************************************************************************
		//Sources 
		System.out.println("	Audio Source gain configuration");
		for(int a=0; a<=1; a++) //2 iterations, one to set and one to check
		{
			paSourceList.clear();
			paSourceList = pac.getPaSources(); //refresh source list to get new gains
			
			for(int i=0; i < paSourceList.size(); i++) 
			{
				paSource = paSourceList.get(i);
				if(Integer.toString(paSource.getId()).contains(dest)) //partial matching device sources
				{
					assertEquals(CURR_NAC_IP, paSource.getIpAddress(), "Audio Source ID " + paSource.getId() + "IP inconsistent with device");
					if(paSource.getType().name().equals("NETSPIRE_ANALOG_INPUT")) //only count analog outs 
					{
						if (a==0) //first iteration, set gains
						{
							sourceCount++;
							paSource.setGain(DEF_GAIN, true, false);
		
							try { Thread.sleep(10000); }
							catch (InterruptedException e) { }		
						}
						else {
							assertEquals(DEF_GAIN, paSource.getGain(), "Audio Source ID " + paSource.getId() + " gain not set correctly" ); //second iteration, check gains
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

		
		//Sinks	
		System.out.println("	Audio Sink gain configuration");
		for (int a=0; a<=1; a++) 
		{
			paSinkList.clear();
			paSinkList = pac.getPaSinks(); //refresh sink list to get new gains
			for (int i=0; i < paSinkList.size(); i++) 
			{
				paSink = paSinkList.get(i);
				if(Integer.toString(paSink.getId()).contains(dest)) //partial matching device sinks
				{
					assertEquals(CURR_NAC_IP, paSink.getIpAddress(), "Audio Sink ID " + paSink.getId() + "IP inconsistent with device"); //check IP of each sink against device IP
					if (a==0)  //first iteration, set gains
					{
						sinkCount++;
						paSink.setGain(sinkGain, true, false);
						
						try { Thread.sleep(5000); }
						catch (InterruptedException e) { }
					}
					else 
					{
						assertEquals(DEF_GAIN, paSink.getGain().getLevel(), "Audio Sink ID " + paSink.getId() + " gain not set correctly"); //second iteration, check gains
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
		System.out.println("Audio input/output pass");
		System.out.println();
		//********************************************************************************/	
		
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
		outputVisualList.clear(); //no visual zones used in this test
		Gain gain = new Gain();
		
		//Testing zones
		PaZoneArray paZoneList = new PaZoneArray();
		PaZone zone = new PaZone(); 
		PaZone allZone = new PaZone();
		
		paZoneList.clear();
		paZoneList = pac.getPaZones();
		
		System.out.println("Testing PA single zone play back");
		
		//Testing playback from one zone at a time
		for (int i=0; i < paZoneList.size(); i++) 
		{
			zone = paZoneList.get(i);
			
			if (zone.getId().contains("Test/Test")) //all local zones 
			{
				outputZoneList.clear(); //use one test zone at a time
				outputZoneList.add(zone.getId());
				pac.playMessage(outputZoneList, outputVisualList, gain, dvaItems, null, false, false, 0, 0, 0); //play to zone
				
				try { Thread.sleep(500); }
				catch (InterruptedException e) { }
				
				if(Character.isDigit(zone.getId().charAt(9))) //for single sink zones
				{
					paSinkList.clear();
					paSinkList = zone.getMembers(); //reusing variable from before
					assertEquals(1, paSinkList.size()); 
					
					paSink = paSinkList.get(0); //should only be one 
					
					//check message is being played 
					assertEquals(AnnouncementType.FILE_PLAY, paSink.getAnnouncementType());
					assertEquals(State.ACTIVE, paSink.getState());
		
					try { Thread.sleep(8000); }
					catch (InterruptedException e) { }
				}
				else //Test/TestAll 
				{
					try { Thread.sleep(3000); } //some delay in multi-zone playback, before all zones are active
					catch (InterruptedException e) { } //NOTE this may cause inconsistencies
					
					paSinkList.clear();
					paSinkList = zone.getMembers();
					for(int j=0; j<paSinkList.size(); j++)  //all zones should be active
					{
						paSink = paSinkList.get(j);
						assertEquals(AnnouncementType.FILE_PLAY, paSink.getAnnouncementType());
						assertEquals(State.ACTIVE, paSink.getState());
					}
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
		//how to confirm queuing works?? possibly queue to different output zones and test 
		
		System.out.println("Testing PA queued message play back");
		
		outputZoneList.clear();
		outputZoneList.add("Test/Test1");
		pac.playMessage(outputZoneList, outputVisualList, gain, dvaItems, null, false, false, 0, 0, 0);
		outputZoneList.clear();
		outputZoneList.add("Test/Test1");
		pac.playMessage(outputZoneList, outputVisualList, gain, dvaItems, null, false, false, 0, 0, 0);
		outputZoneList.clear();
		outputZoneList.add("Test/Test1");
		pac.playMessage(outputZoneList, outputVisualList, gain, dvaItems, null, false, false, 0, 0, 0);
		outputZoneList.clear();
		outputZoneList.add("Test/Test1");
		pac.playMessage(outputZoneList, outputVisualList, gain, dvaItems, null, false, false, 0, 0, 0);
		
		//check playback in order of queuing?
		
		
		
		
		try { Thread.sleep(30000); }
		catch (InterruptedException e) { }
		
		
		
		//=========================================
        // Test PA System - Live PA SW Trigger
        //=========================================
		
		//Test playback using software trigger from an analog source
		
		System.out.println("Testing live PA software trigger play back");
		
		int sourceId = Integer.parseInt(dest + "01"); //use the first source
		
		//pac.attachPaSource(sourceId); //attach
		
		paSourceList.clear();
		paSourceList = pac.getPaSources();
		
		for(int i=0; i<paSourceList.size(); i++) //retrieve the first PA source 
		{
			paSource = paSourceList.get(i);
			if (paSource.getId() == sourceId) 
			{
				break;
			}
		}
		
		//assertEquals(AttachState.ATTACHED, paSource.getAttachState());
		
		paSource.attachPaZone("Test/TestAll", AttachMode.ADD_TO_EXISTING_SET); //attach zone with all sinks to source
		
		int swTrigId = pac.createSwPaTrigger(sourceId, 100);
		
		assertNotEquals(0,swTrigId);
		
		pac.activateSwPaTrigger(swTrigId);

		try { Thread.sleep(10000); }
		catch (InterruptedException e) { }
		
		paSinkList.clear();
		paSinkList = allZone.getMembers();
		
		for (int i=0; i< paSinkList.size(); i++) { //ensure all sinks are active 
			paSink = paSinkList.get(i);
			assertEquals(AnnouncementType.VOIP_STREAMING, paSink.getAnnouncementType()); //if this fails, restart NAC; SW trigger hung
			assertEquals(State.ACTIVE, paSink.getState());
		}
		pac.deactivateSwPaTrigger(swTrigId);
		//pac.detachPaSource(sourceId);
		
		try { Thread.sleep(5000); }
		catch (InterruptedException e) { }
		
		System.out.println("All PA tets okay");
		System.out.println();
		
		
		System.out.println("All tests okay");
		as.disconnect(); 
	}
	
	
	private Device refreshDevice (AudioServer as) 
	{
		DeviceStateArray devices = new DeviceStateArray();
		Device d = new Device(); //test device
		
		while(true) 
		{
			devices.clear();
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
	 * Muting/unmuting PA sinks //SDK implementation incomplete
	 * Muting/unmuting PA zones // '' 
	 * 
	 * Test attaching/detaching sources
	 * 
	 * Playback using messages with different priorities, playback using actual Message object/class
	 * 
	 * Message cancellation
	 * 
	 * Hardware triggers for source??
	 */
	
}
