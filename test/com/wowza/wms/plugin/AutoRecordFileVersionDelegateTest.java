package com.wowza.wms.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.application.WMSProperties;
import com.wowza.wms.livestreamrecord.manager.IStreamRecorder;
import com.wowza.wms.livestreamrecord.manager.StreamRecorderParameters;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AutoRecordFileVersionDelegateTest {

    private static final String STREAM_NAME = "myStream";
    private static final long START_MILLIS = 1791000000000L; // fixed recording start time

    @TempDir
    Path tempDir;

    private AutoRecordFileVersionDelegate delegate;
    private IStreamRecorder recorder;
    private StreamRecorderParameters params;
    private WMSProperties streamRecorderProps;

    @BeforeEach
    void setUp() {
        delegate = new AutoRecordFileVersionDelegate();

        streamRecorderProps = new WMSProperties();
        IApplicationInstance appInstance = mock(IApplicationInstance.class);
        when(appInstance.getStreamRecorderProperties()).thenReturn(streamRecorderProps);

        // StreamRecorderParameters has no default constructor, mock it and set the public field directly
        params = mock(StreamRecorderParameters.class);

        recorder = mock(IStreamRecorder.class);
        when(recorder.getAppInstance()).thenReturn(appInstance);
        when(recorder.getRecorderParams()).thenReturn(params);
        when(recorder.getStreamName()).thenReturn(STREAM_NAME);
        when(recorder.getBaseFilePath()).thenReturn(tempDir.resolve(STREAM_NAME + ".mp4").toString());
        when(recorder.getStartTime()).thenReturn(new DateTime(START_MILLIS));
        when(recorder.getSegmentNumber()).thenReturn(3);
    }

    private static String format(String pattern, long millis) {
        ZonedDateTime time = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault());
        return DateTimeFormatter.ofPattern(pattern).format(time);
    }

    private String expectedPath(String fileName) {
        return tempDir + File.separator + fileName;
    }

    @Test
    void usesDefaultTemplateAndFormatWhenNotSet() {
        String expected = STREAM_NAME + "_" + format(AutoRecordFileVersionDelegate.DEFAULT_DATETIME_FORMAT, START_MILLIS) + "_3.mp4";
        assertEquals(expectedPath(expected), delegate.getFilename(recorder));
    }

    @Test
    void usesTemplateAndDateTimeFormatFromProperties() {
        params.fileTemplate = "${SourceStreamName}_${RecordingStartTime}";
        streamRecorderProps.setProperty(AutoRecordFileVersionDelegate.PROP_DATETIME_FORMAT, "yyyyMMdd_HHmm");

        String expected = STREAM_NAME + "_" + format("yyyyMMdd_HHmm", START_MILLIS) + ".mp4";
        assertEquals(expectedPath(expected), delegate.getFilename(recorder));
    }

    @Test
    void replacesBaseFileNameAndSegmentNumber() {
        when(recorder.getBaseFilePath()).thenReturn(tempDir.resolve("baseName.flv").toString());
        params.fileTemplate = "${BaseFileName}-${SourceStreamName}-${SegmentNumber}";

        assertEquals(expectedPath("baseName-" + STREAM_NAME + "-3.flv"), delegate.getFilename(recorder));
    }

    @Test
    void segmentTimeUsesCurrentTime() {
        params.fileTemplate = "${SegmentTime}";
        streamRecorderProps.setProperty(AutoRecordFileVersionDelegate.PROP_DATETIME_FORMAT, "yyyyMMddHHmmss");

        long before = System.currentTimeMillis() / 1000 * 1000;
        String name = delegate.getFilename(recorder);
        long after = System.currentTimeMillis();

        String timeStr = new File(name).getName().replace(".mp4", "");
        long segmentMillis = ZonedDateTime.parse(timeStr, DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.systemDefault())).toInstant().toEpochMilli();
        assertTrue(segmentMillis >= before && segmentMillis <= after, "SegmentTime " + timeStr + " is not the current time");
    }

    @Test
    void stripsExtensionFromTemplate() {
        params.fileTemplate = "${SourceStreamName}_${SegmentNumber}.mp4";

        assertEquals(expectedPath(STREAM_NAME + "_3.mp4"), delegate.getFilename(recorder));
    }

    @Test
    void fallsBackToDefaultFormatWhenFormatInvalid() {
        params.fileTemplate = "${RecordingStartTime}";
        streamRecorderProps.setProperty(AutoRecordFileVersionDelegate.PROP_DATETIME_FORMAT, "yyyy'MM");

        String expected = format(AutoRecordFileVersionDelegate.DEFAULT_DATETIME_FORMAT, START_MILLIS) + ".mp4";
        assertEquals(expectedPath(expected), delegate.getFilename(recorder));
    }

    @Test
    void usesDefaultFormatWhenNoAppInstance() {
        when(recorder.getAppInstance()).thenReturn(null);
        params.fileTemplate = "${RecordingStartTime}";

        String expected = format(AutoRecordFileVersionDelegate.DEFAULT_DATETIME_FORMAT, START_MILLIS) + ".mp4";
        assertEquals(expectedPath(expected), delegate.getFilename(recorder));
    }

    @Test
    void usesDefaultTemplateWhenNoRecorderParams() {
        when(recorder.getRecorderParams()).thenReturn(null);
        streamRecorderProps.setProperty(AutoRecordFileVersionDelegate.PROP_DATETIME_FORMAT, "yyyyMMdd");

        String expected = STREAM_NAME + "_" + format("yyyyMMdd", START_MILLIS) + "_3.mp4";
        assertEquals(expectedPath(expected), delegate.getFilename(recorder));
    }

    @Test
    void addsVersionSuffixWhenFileExistsAndVersioningEnabled() throws IOException {
        params.fileTemplate = "${SourceStreamName}";
        when(recorder.isVersionFile()).thenReturn(true);

        assertEquals(expectedPath(STREAM_NAME + ".mp4"), delegate.getFilename(recorder));

        tempDir.resolve(STREAM_NAME + ".mp4").toFile().createNewFile();
        assertEquals(expectedPath(STREAM_NAME + "_0.mp4"), delegate.getFilename(recorder));

        tempDir.resolve(STREAM_NAME + "_0.mp4").toFile().createNewFile();
        assertEquals(expectedPath(STREAM_NAME + "_1.mp4"), delegate.getFilename(recorder));
    }

    @Test
    void keepsExistingFileNameWhenVersioningDisabled() throws IOException {
        params.fileTemplate = "${SourceStreamName}";
        when(recorder.isVersionFile()).thenReturn(false);
        File existing = tempDir.resolve(STREAM_NAME + ".mp4").toFile();
        existing.createNewFile();

        assertEquals(existing.getPath(), delegate.getFilename(recorder));
        assertTrue(existing.exists(), "existing file must not be deleted");
    }

    @Test
    void returnsTempFileNameOnError() {
        when(recorder.getBaseFilePath()).thenReturn(null);

        assertEquals("junk.tmp", delegate.getFilename(recorder));
    }
}
