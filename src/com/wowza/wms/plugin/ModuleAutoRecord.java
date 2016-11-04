package com.wowza.wms.plugin;

import com.wowza.util.StringUtils;
import com.wowza.wms.application.*;
import com.wowza.wms.livestreamrecord.manager.*;
import com.wowza.wms.module.*;

public class ModuleAutoRecord extends ModuleBase
{
	public void onAppStart(IApplicationInstance appInstance)
	{
		// Create a new StreamRecorderParameters object with defaults set via StreamRecorder Properties in the application.
		StreamRecorderParameters recordParams = new StreamRecorderParameters(appInstance);
		
		boolean recordAllStreams = appInstance.getStreamRecorderProperties().getPropertyBoolean("streamRecorderRecordAllStreams", true);
		recordAllStreams = appInstance.getProperties().getPropertyBoolean("streamRecorderRecordAllStreams", recordAllStreams);
		
		if(recordAllStreams)
		{
			// Automatically record all streams as they are published. 
			// Recorders will only be created when a stream is first published.
			appInstance.getVHost().getLiveStreamRecordManager().startRecording(appInstance, recordParams);
		}
		else
		{
			// Read a pipe (|) or comma separated list of names from properties and just start recorders for each of these names
			// Recorders will be created immediatly for each name and will wait for the stream to be published.
			String namesStr = appInstance.getStreamRecorderProperties().getPropertyStr("streamRecorderStreamNames", null);
			namesStr = appInstance.getProperties().getPropertyStr("streamRecorderStreamNames", namesStr);
		
			if(!StringUtils.isEmpty(namesStr))
			{
				String[] names = namesStr.split("(\\||,)");
				for(String name : names)
				{
					appInstance.getVHost().getLiveStreamRecordManager().startRecording(appInstance, name.trim(), recordParams);
				}
			}
		}
	}
}