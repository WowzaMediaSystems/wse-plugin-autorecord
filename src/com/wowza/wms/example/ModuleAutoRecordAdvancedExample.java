/*
 * This code and all components (c) Copyright 2006 - 2018, Wowza Media Systems, LLC. All rights reserved.
 * This code is licensed pursuant to the Wowza Public License version 1.0, available at www.wowza.com/legal.
 */
package com.wowza.wms.example;

import java.io.*;

import org.joda.time.*;

import com.wowza.wms.application.*;
import com.wowza.wms.livestreamrecord.manager.*;
import com.wowza.wms.logging.*;
import com.wowza.wms.module.*;
import com.wowza.wms.stream.*;
import com.wowza.wms.vhost.*;

/**
 * Example module that shows how to implement some advanced StreamRecorder features.
 *
 */
public class ModuleAutoRecordAdvancedExample extends ModuleBase implements IModuleOnApp
{
	private IApplicationInstance appInstance = null;
	private IVHost vhost = null;
	private boolean recordAllStreams = true;
	
	// custom File version delegate
	class MyFileVersionDelegate implements IStreamRecorderFileVersionDelegate
	{
		public String getFilename(IStreamRecorder recorder) 
		{
			String name;
	
			try
			{
				File file = new File(recorder.getBaseFilePath());
				String oldBasePath = file.getParent();
				String oldName = file.getName();
				String oldExt = "";
				int oldExtIndex = oldName.lastIndexOf(".");
				if (oldExtIndex >= 0)
				{
					oldExt = oldName.substring(oldExtIndex);
					oldName = oldName.substring(0, oldExtIndex);
				}
		
				name = oldBasePath+"/"+oldName+"_"+DateTime.now().millisOfDay().getAsText()+oldExt;
				file = new File(name);
				if (file.exists())
				{
					file.delete();
				}
				
			} 
			catch (Exception e)
			{
				WMSLoggerFactory.getLogger(MyFileVersionDelegate.class).error("LiveStreamRecordFileVersionDelegate.getFilename: "+e.toString());
				// return a temp filename
				name = "junk.tmp";
			}
	
			return name;
		}
	}
	
	// listener class for IStreamRecorder events
	class MyStreamRecorderListener implements IStreamRecorderActionNotify
	{
		@Override
		public void onCreateRecorder(IStreamRecorder recorder)
		{
			/*
			To set stream specific StreamRecorderParameter values, set them here
			if (recorder.getStreamName().equals("mySpeciaStream"))
			{
				StreamRecorderParameters params = recorder.getRecorderParams();
				params.fileVersionDelegate = new SpecialStreamFileVersionDelegate();
				params.notifyListener = new SpecialStreamOtherListener();
			}
			*/
			getLogger().info("MyStreamRecorderListener.onCreateRecorder[" + appInstance.getContextStr() + "]: new Recording created:" + recorder.getStreamName());
		}

		@Override
		public void onStartRecorder(IStreamRecorder recorder)
		{
		    // log where the recording is going to being written
			getLogger().info("MyStreamRecorderListener.onStartRecorder[" + appInstance.getContextStr() + "]: new Recording started:" + recorder.getStreamName() + " " + recorder.getFilePath());
		}

		@Override
		public void onSplitRecorder(IStreamRecorder recorder)
		{
			getLogger().info("MyStreamRecorderListener.onSplitRecorder[" + appInstance.getContextStr() + "]: Segment recording:" + recorder.getStreamName());
		}

		@Override
		public void onStopRecorder(IStreamRecorder recorder)
		{
			getLogger().info("MyStreamRecorderListener.onStopRecorder[" + appInstance.getContextStr() + "]: Recording stopped:" + recorder.getStreamName() + " " + recorder.getCurrentFile());
		}

		@Override
		public void onSwitchRecorder(IStreamRecorder recorder, IMediaStream newStream)
		{
			getLogger().info("MyStreamRecorderListener.onSwitchRecorder[" + appInstance.getContextStr() + "]: switch to new stream, old Stream:" + recorder.getStreamName() +" new Stream:" + newStream.getName());
		}

		@Override
		public void onSegmentStart(IStreamRecorder recorder)
		{
			getLogger().info("MyStreamRecorderListener.onSegmentStart[" + appInstance.getContextStr() + "]: new segment created:" + recorder.getStreamName());
		}

		@Override
		public void onSegmentEnd(IStreamRecorder recorder)
		{
			getLogger().info("MyStreamRecorderListener.onSegmentEnd[" + appInstance.getContextStr() + "]: segment closed:" + recorder.getStreamName());
		}
	}
	
	public void onAppStart(IApplicationInstance appInstance)
	{
		getLogger().info("ModuleAutoRecordAdvancedExample onAppStart["+appInstance.getContextStr()+"]: ");

     	this.appInstance = appInstance;
     	this.vhost = appInstance.getVHost();
     	
		// Create a new StreamRecorderParameters object with defaults set via properties in application.xml.
		StreamRecorderParameters recordParams = new StreamRecorderParameters(appInstance);
		
		// uncomment and modify each setting below to override default behaviour.
		
		// segment by size, create 60 minute segments using default content path
		//recordParams.segmentationType = IStreamRecorderConstants.SEGMENT_BY_DURATION;
		//recordParams.segmentDuration = 60*60*1000;
		//getLogger().info("--- startRecordingSegmentByDuration for 60 minutes");
		
		// segment by duration, create 1MB segments using default content path
		//recordParams.segmentationType = IStreamRecorderConstants.SEGMENT_BY_DURATION;
		//recordParams.segmentSize = 1024*1024;
		//getLogger().info("--- startRecordingSegmentBySize for 1MB");
		
		// segment by duration, create new segment at 1:00am each day.
		//recordParams.segmentationType = IStreamRecorderConstants.SEGMENT_BY_SCHEDULE;
		//recordParams.segmentSchedule = "0 * 1 * * *";
		//getLogger().info("--- startRecordingSegmentBySchedule every * 1 * * * *");

	    // don't segment, using the default content path, do not append (i.e. overwrite if file exists)
		//recordParams.segmentationType = IStreamRecorderConstants.SEGMENT_NONE;
		//recordParams.versioningOption = IStreamRecorderConstants.OVERWRITE_FILE;
		//getLogger().info("--- startRecording");

		//recordParams.fileFormat = IStreamRecorderConstants.FORMAT_MP4;
		//recordParams.startOnKeyFrame =  true;
		//recordParams.recordData = true;

		// set custom file version delegate
		//recordParams.fileVersionDelegate = new MyFileVersionDelegate();
		// add recorder listener
		//recordParams.notifyListener = new MyStreamRecorderListener();

		if(this.recordAllStreams)
		{
			// tell LiveStreamRecordManager to record all streams for this Instance using these params
			vhost.getLiveStreamRecordManager().startRecording(appInstance, recordParams);
		}
		else
		{
			// or tell it to record a named stream. The recorder will start immediately and wait for the stream to be published.
			vhost.getLiveStreamRecordManager().startRecording(appInstance, "myStream", recordParams);
		}
	}

	public void onAppStop(IApplicationInstance appInstance)
	{
		// Nothing to do here because LiveStreamRecord Manager stops and destroys the Stream Recorders when the Application stops.
	}
}
