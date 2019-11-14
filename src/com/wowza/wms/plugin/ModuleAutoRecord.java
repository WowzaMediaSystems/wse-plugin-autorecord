/*
 * This code and all components (c) Copyright 2006 - 2019, Wowza Media Systems, LLC. All rights reserved.
 * This code is licensed pursuant to the Wowza Public License version 1.0, available at www.wowza.com/legal.
 */
package com.wowza.wms.plugin;

import java.util.regex.PatternSyntaxException;

import com.wowza.util.StringUtils;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.livestreamrecord.manager.ILiveStreamRecordManager;
import com.wowza.wms.livestreamrecord.manager.StreamRecorderParameters;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.logging.WMSLoggerIDs;
import com.wowza.wms.module.ModuleBase;
import com.wowza.wms.stream.IMediaStream;
import com.wowza.wms.stream.MediaStreamActionNotifyBase;
import com.wowza.wms.vhost.IVHost;

public class ModuleAutoRecord extends ModuleBase
{
	public static final String CLASSNAME = "ModuleAutoRecord";

	private enum RecordType
	{
		all, source, transcoder, allow, whitelist, deny, blacklist, none;
	}

	private WMSLogger logger = null;
	private IApplicationInstance appInstance = null;
	private IVHost vhost = null;
	private StreamListener actionNotify = new StreamListener();

	private String namesStr = null;
	private String namesStrDelimiter = "(\\||,)";
	private RecordType recordType = RecordType.all;

	private Boolean debugLog = false;
	private StreamRecorderParameters recordParams = null;
	private boolean shutDownRecorderOnUnPublish = true;

	public void onAppCreate(IApplicationInstance appInstance)
	{
		logger = WMSLoggerFactory.getLoggerObj(appInstance);
		logger.info(CLASSNAME + ".onAppCreate[" + appInstance.getContextStr() + "] Build #6", WMSLoggerIDs.CAT_application, WMSLoggerIDs.EVT_comment);

		this.appInstance = appInstance;
		vhost = appInstance.getVHost();

		// main streamRecorder debugLog property
		debugLog = appInstance.getStreamRecorderProperties().getPropertyBoolean("streamRecorderDebugEnable", debugLog);

		// local debugLog property
		debugLog = appInstance.getStreamRecorderProperties().getPropertyBoolean("streamRecorderAutoRecordDebugLog", debugLog);

		if (logger.isDebugEnabled())
			debugLog = true;

		String streamType = appInstance.getStreamType();
		if(streamType.contains("-record"))
		{
			String newStreamType = streamType.replace("-record", "");
			appInstance.setStreamType(newStreamType);

			logger.info(CLASSNAME + ".onAppCreate[" + appInstance.getContextStr() + "] Application has " + streamType + " stream type set. Changing to " + newStreamType + " stream type to prevent conflict.", WMSLoggerIDs.CAT_application, WMSLoggerIDs.EVT_comment);
		}

		try
		{
			recordType = RecordType.valueOf(appInstance.getStreamRecorderProperties().getPropertyStr("streamRecorderRecordType", recordType.toString()).toLowerCase());
		}
		catch (Exception e)
		{
			logger.warn(CLASSNAME + ".onAppCreate[" + appInstance.getContextStr() + "] streamRecorderRecordType value not correct. Disabling Automatic recording", WMSLoggerIDs.CAT_application, WMSLoggerIDs.EVT_comment);
			recordType = RecordType.none;
		}
		namesStr = appInstance.getStreamRecorderProperties().getPropertyStr("streamRecorderStreamNames", null);
		namesStrDelimiter = appInstance.getStreamRecorderProperties().getPropertyStr("streamRecorderStreamNamesDelimiter", namesStrDelimiter);

		// Create a new StreamRecorderParameters object with defaults set via StreamRecorder Properties in the application.
		recordParams = new StreamRecorderParameters(appInstance);

		boolean recordAllStreams = appInstance.getStreamRecorderProperties().getPropertyBoolean("streamRecorderRecordAllStreams", recordType == RecordType.all);
		recordAllStreams = appInstance.getProperties().getPropertyBoolean("streamRecorderRecordAllStreams", recordAllStreams);
		if(recordAllStreams)
			recordType = RecordType.all;

		if (recordType == RecordType.all)
		{
			// Automatically record all streams as they are published.
			// Recorders will only be created when a stream is first published.
			appInstance.getVHost().getLiveStreamRecordManager().startRecording(appInstance, recordParams);
			logger.info(CLASSNAME + ".onAppCreate[" + appInstance.getContextStr() + "] recording all streams", WMSLoggerIDs.CAT_application, WMSLoggerIDs.EVT_comment);
		}

		// Recorders will normally remain in memory after the stream is unpublished so they can easily be reused.
		// Use streamRecorderStopRecorderOnUnPublish to stop the recorder when the stream is unpublished.
		// The recorder will then be recreated when the stream is restarted.
		shutDownRecorderOnUnPublish = appInstance.getStreamRecorderProperties().getPropertyBoolean("streamRecorderShutDownRecorderOnUnPublish", shutDownRecorderOnUnPublish);
		shutDownRecorderOnUnPublish = appInstance.getProperties().getPropertyBoolean("streamRecorderShutDownRecorderOnUnPublish", shutDownRecorderOnUnPublish);
		logger.info(CLASSNAME + ".onAppCreate[" + appInstance.getContextStr() + "] recorders will " + (!shutDownRecorderOnUnPublish ? "not " : "") + "be shut down on unpublish", WMSLoggerIDs.CAT_application, WMSLoggerIDs.EVT_comment);
	}

	class StreamListener extends MediaStreamActionNotifyBase
	{
		@Override
		public void onPublish(IMediaStream stream, String streamName, boolean isRecord, boolean isAppend)
		{
			if(recordType == RecordType.all || vhost.getLiveStreamRecordManager().getRecorder(appInstance, streamName) != null)
				return;

			boolean matchFound = false;
			boolean canRecord = false;
			switch (recordType)
			{
			case allow:
			case whitelist:
				matchFound = checkNames(streamName);
				if (matchFound)
					canRecord = true;
				break;

			case deny:
			case blacklist:
				matchFound = checkNames(streamName);
				if (!matchFound)
					canRecord = true;
				break;

			case source:
				if (!stream.isTranscodeResult())
					canRecord = true;
				break;

			case transcoder:
				if (stream.isTranscodeResult())
					canRecord = true;
				break;

			case none:
			default:
				break;
			}

			if (canRecord)
			{
				if (debugLog)
					logger.info(CLASSNAME + ".onPublish [" + appInstance.getContextStr() + "/" + streamName + "] starting recording. RecordType: " + recordType, WMSLoggerIDs.CAT_application, WMSLoggerIDs.EVT_comment);
				vhost.getLiveStreamRecordManager().startRecording(appInstance, streamName, recordParams);
			}
			else if(debugLog)
				logger.info(CLASSNAME + ".onPublish [" + appInstance.getContextStr() + "/" + streamName + "] not starting recording. RecordType: " + recordType, WMSLoggerIDs.CAT_application, WMSLoggerIDs.EVT_comment);
		}

		private boolean checkNames(String streamName)
		{
			boolean matchFound = false;

			if (!StringUtils.isEmpty(namesStr))
			{
				while (true)
				{
					if (namesStr.equals("*"))
					{
						if (debugLog)
							logger.info(CLASSNAME + ".checkNames [" + appInstance.getContextStr() + "/" + streamName + "] match found against *", WMSLoggerIDs.CAT_application, WMSLoggerIDs.EVT_comment);
						matchFound = true;
						break;
					}
					// Read a pipe (|) or comma separated list of names from properties and start recorder if it matches
					String[] names = namesStr.split(namesStrDelimiter);
					for (String name : names)
					{
						name = name.trim();
						if (debugLog)
							logger.info(CLASSNAME + ".checkNames [" + appInstance.getContextStr() + "/" + streamName + "] regex check against " + name, WMSLoggerIDs.CAT_application, WMSLoggerIDs.EVT_comment);
						// wildcard suffix match
						if (name.startsWith("*"))
						{
							if (debugLog)
								logger.info(CLASSNAME + ".checkNames [" + appInstance.getContextStr() + "/" + streamName + "] wildcard suffix check against " + name, WMSLoggerIDs.CAT_application, WMSLoggerIDs.EVT_comment);
							name = name.substring(1);
							if (streamName.endsWith(name))
							{
								if (debugLog)
									logger.info(CLASSNAME + ".checkNames [" + appInstance.getContextStr() + "/" + streamName + "] wildcard suffix match found against " + name, WMSLoggerIDs.CAT_application, WMSLoggerIDs.EVT_comment);
								matchFound = true;
								break;
							}
						}
						// wildcard prefix match
						if (name.endsWith("*"))
						{
							if (debugLog)
								logger.info(CLASSNAME + ".checkNames [" + appInstance.getContextStr() + "/" + streamName + "] wildcard prefix check against " + name, WMSLoggerIDs.CAT_application, WMSLoggerIDs.EVT_comment);
							name = name.substring(0, name.length() - 1);
							if (streamName.startsWith(name))
							{
								if (debugLog)
									logger.info(CLASSNAME + ".checkNames [" + appInstance.getContextStr() + "/" + streamName + "] wildcard prefix match found against " + name, WMSLoggerIDs.CAT_application, WMSLoggerIDs.EVT_comment);
								matchFound = true;
								break;
							}
						}
						// regex match
						try {
							if (streamName.matches(name))
							{
								if (debugLog)
									logger.info(CLASSNAME + ".checkNames [" + appInstance.getContextStr() + "/" + streamName + "] regex match found against " + name, WMSLoggerIDs.CAT_application, WMSLoggerIDs.EVT_comment);
								matchFound = true;
								break;
							}
						} catch (PatternSyntaxException e) {
							if (debugLog)
								logger.warn(CLASSNAME + ".checkNames [" + appInstance.getContextStr() + "/" + streamName + "] exception: " + e.getMessage(), WMSLoggerIDs.CAT_application, WMSLoggerIDs.EVT_comment);
						}
					}
					break;
				}
			}
			else
			{
				if (debugLog)
					logger.info(CLASSNAME + ".checkNames [" + appInstance.getContextStr() + "/" + streamName + "] streamRecorderStreamNames list is empty", WMSLoggerIDs.CAT_application, WMSLoggerIDs.EVT_comment);
			}

			return matchFound;
		}

		@Override
		public void onUnPublish(IMediaStream stream, String streamName, boolean isRecord, boolean isAppend)
		{
			if(shutDownRecorderOnUnPublish)
			{
				if (debugLog)
					logger.info(CLASSNAME + ".onUnPublish [" + appInstance.getContextStr() + "/" + streamName + "] shutting down recorder", WMSLoggerIDs.CAT_application, WMSLoggerIDs.EVT_comment);
				vhost.getLiveStreamRecordManager().stopRecording(appInstance, streamName);
			}
		}
	}

	public void onStreamCreate(IMediaStream stream)
	{
		stream.addClientListener(actionNotify);
	}
}
