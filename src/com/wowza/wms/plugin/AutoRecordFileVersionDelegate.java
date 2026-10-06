package com.wowza.wms.plugin;

import com.wowza.util.StringUtils;
import com.wowza.wms.application.WMSProperties;
import com.wowza.wms.livestreamrecord.manager.IStreamRecorder;
import com.wowza.wms.livestreamrecord.manager.IStreamRecorderFileVersionDelegate;
import com.wowza.wms.logging.WMSLoggerFactory;
import java.io.File;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * File version delegate that names files using a template with a configurable date/time format.
 *
 * StreamRecorder properties:
 *   streamRecorderFileVersionTemplate - file name template (WSE property, read via StreamRecorderParameters.fileTemplate).
 *       Supported tags: ${SourceStreamName}, ${BaseFileName}, ${SegmentNumber}, ${RecordingStartTime}, ${SegmentTime}.
 *       Default: ${SourceStreamName}_${RecordingStartTime}_${SegmentNumber}
 *   streamRecorderFileVersionDateTimeFormat - Joda-Time pattern used for ${RecordingStartTime} and ${SegmentTime}, e.g. MMddyyyy.
 *       Default: yyyy-MM-dd-HH.mm.ss.SSS-z
 */
public class AutoRecordFileVersionDelegate implements IStreamRecorderFileVersionDelegate {

    public static final String PROP_DATETIME_FORMAT = "streamRecorderFileVersionDateTimeFormat";

    public static final String DEFAULT_FILE_TEMPLATE = "${SourceStreamName}_${RecordingStartTime}_${SegmentNumber}";
    public static final String DEFAULT_DATETIME_FORMAT = "yyyy-MM-dd-HH.mm.ss.SSS-z";

    public static final String STREAM_NAME_TAG = "${SourceStreamName}";
    public static final String BASE_NAME_TAG = "${BaseFileName}";
    public static final String SEGMENT_NUMBER_TAG = "${SegmentNumber}";
    public static final String START_TIME_TAG = "${RecordingStartTime}";
    public static final String SEGMENT_TIME_TAG = "${SegmentTime}";

    public String getFilename(IStreamRecorder recorder) {
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

            String template = recorder.getRecorderParams() != null ? recorder.getRecorderParams().fileTemplate : null;
            if (StringUtils.isEmpty(template))
                template = DEFAULT_FILE_TEMPLATE;
            // the extension is added from the base file path
            if (template.endsWith(".mp4") || template.endsWith(".flv"))
                template = template.substring(0, template.length() - 4);

            DateTimeFormatter formatter = getDateTimeFormatter(recorder);
            DateTime startTime = recorder.getStartTime() != null ? recorder.getStartTime() : DateTime.now();

            String fileName = template
                    .replace(STREAM_NAME_TAG, recorder.getStreamName())
                    .replace(BASE_NAME_TAG, oldName)
                    .replace(START_TIME_TAG, formatter.print(startTime))
                    .replace(SEGMENT_TIME_TAG, formatter.print(DateTime.now()))
                    .replace(SEGMENT_NUMBER_TAG, String.valueOf(recorder.getSegmentNumber()));

            name = oldBasePath + File.separator + fileName + oldExt;

            // if versioning is enabled, don't overwrite an existing file
            if (recorder.isVersionFile())
            {
                int version = 0;
                while (new File(name).exists())
                {
                    name = oldBasePath + File.separator + fileName + "_" + version + oldExt;
                    version++;
                }
            }
        }
        catch (Exception e)
        {
            WMSLoggerFactory.getLogger(AutoRecordFileVersionDelegate.class).error("AutoRecordFileVersionDelegate.getFilename: "+e.toString());
            // return a temp filename
            name = "junk.tmp";
        }

        return name;
    }

    private DateTimeFormatter getDateTimeFormatter(IStreamRecorder recorder) {
        String format = DEFAULT_DATETIME_FORMAT;
        if (recorder.getAppInstance() != null)
        {
            WMSProperties props = recorder.getAppInstance().getStreamRecorderProperties();
            format = props.getPropertyStr(PROP_DATETIME_FORMAT, format);        }

        try
        {
            return DateTimeFormat.forPattern(format);
        }
        catch (IllegalArgumentException e)
        {
            WMSLoggerFactory.getLogger(AutoRecordFileVersionDelegate.class).warn("AutoRecordFileVersionDelegate.getDateTimeFormatter: invalid " + PROP_DATETIME_FORMAT + " [" + format + "], using default: " + e.getMessage());
            return DateTimeFormat.forPattern(DEFAULT_DATETIME_FORMAT);
        }
    }
}
