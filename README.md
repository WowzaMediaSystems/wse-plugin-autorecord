# LiveStreamAutoRecord
**ModuleAutoRecord** for [Wowza Streaming Engine™ media server software](https://www.wowza.com/products/streaming-engine) is an alternative to the [Record all incoming streams](https://www.wowza.com/forums/content.php?43-How-to-set-up-live-video-recording) option in the Wowza streaming Engine Manager, that enables you to automatically record all or selected incoming streams on an application using the default StreamRecorder parameters configured for the application. The recordings are accessible via the Wowza Streaming Engine Manager interface or REST Service as well as the LiveStreamRecord HTTPProvider.

## Prerequisites
Wowza Streaming Engine 4.0.0 or later is required.

## Usage
When the application is started, the module will check to see if the `streamRecorderRecordAllStreams` property is set to true (default) and if so it will set the Stream Recorder Manager to record all of the streams that are published to the application. If `streamRecorderRecordAllStreams` is set to false then the module will check to see if the `streamRecorderStreamNames` property contains a list of names to record and if so will start a recorder for each of these.
Each recorder that is started will use the default Stream Recorder Parameters that are configured for the application. These can be set via the StreamRecorder properties. See [How to record live streams (Wowza Streaming Engine)](https://www.wowza.com/forums/content.php?574-How-to-record-live-streams-%28Wowza-Streaming-Engine%29)

## More resources
[Wowza Streaming Engine Server-Side API Reference](https://www.wowza.com/resources/WowzaStreamingEngine_ServerSideAPI.pdf)

[How to extend Wowza Streaming Engine using the Wowza IDE](https://www.wowza.com/forums/content.php?759-How-to-extend-Wowza-Streaming-Engine-using-the-Wowza-IDE)

Wowza Media Systems™ provides developers with a platform to create streaming applications and solutions. See [Wowza Developer Tools](https://www.wowza.com/resources/developers) to learn more about our APIs and SDK.

To use the compiled version of this module, see [How to start and stop live stream recordings programmatically (LiveStreamRecordAutoRecord)](https://www.wowza.com/forums/content.php?576-How-to-start-and-stop-live-stream-recordings-programmatically-%28LiveStreamRecordAutoRecord%29).


## Contact
[Wowza Media Systems, LLC](https://www.wowza.com/contact)

## License
This code is distributed under the [Wowza Public License](https://github.com/WowzaMediaSystems/wse-plugin-avmix/blob/master/LICENSE.txt).

![alt tag](http://wowzalogs.com/stats/githubimage.php?plugin=wse-plugin-autorecord)