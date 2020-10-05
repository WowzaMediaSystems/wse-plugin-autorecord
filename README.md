# LiveStreamAutoRecord
**ModuleAutoRecord** for Wowza Streaming Engine [media server software](https://www.wowza.com/products/streaming-engine) is an alternative to the [Record all incoming streams](https://www.wowza.com/docs/how-to-record-live-streams-wowza-streaming-engine) option in Wowza streaming Engine Manager. It enables you to record either all or selected incoming streams on an application automatically using the default StreamRecorder parameters configured for the application. The recordings are accessible via the Wowza Streaming Engine Manager user interface, the Wowza Streaming Engine REST Service, and the **LiveStreamRecord** HTTP provider.

This repo includes a [compiled version](/lib/wse-plugin-autorecord.jar).

## Prerequisites
Wowza Streaming Engine™ 4.0.0 or later is required.

## Usage
When the application is started, the module checks to see if the `streamRecorderRecordAllStreams` property is set to **true** (default) or if the `streamRecorderRecordType` property is set to **all**, and if so, it sets the Stream Recorder Manager to record all of the streams that are published to the application. Alternatively, streams are recorded based on the `streamRecorderRecordType` setting and if a match is found in the `streamRecorderStreamNames` list.

Each recorder that's started uses the default StreamRecorder parameters that are configured for the application. These can be set via the [StreamRecorder properties](https://www.wowza.com/docs/how-to-record-live-streams-wowza-streaming-engine#livestreamrecordproperties). For more information, see [How to record live streams (Wowza Streaming Engine)](https://www.wowza.com/docs/how-to-record-live-streams-wowza-streaming-engine)

## More resources
To use the compiled version of this module, see [How to start recording streams automatically (LiveStreamRecordAutoRecord)](https://www.wowza.com/docs/how-to-start-recording-streams-automatically-livestreamrecordautorecord).

[Wowza Streaming Engine Server-Side API Reference](https://www.wowza.com/resources/serverapi/)

[How to extend Wowza Streaming Engine using the Wowza IDE](https://www.wowza.com/docs/how-to-extend-wowza-streaming-engine-using-the-wowza-ide)

Wowza Media Systems™ provides developers with a platform to create streaming applications and solutions. See [Wowza Developer Tools](https://www.wowza.com/developer) to learn more about our APIs and SDK.

## Contact
[Wowza Media Systems, LLC](https://www.wowza.com/contact)

## License
This code is distributed under the [Wowza Public License](/LICENSE.txt).
