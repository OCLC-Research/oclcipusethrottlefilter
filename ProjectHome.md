A servlet filter that restricts a remote IP address to a configurable number of simultaneous requests.  When that number is exceeded, the client receives a 403 (Forbidden) response and a message body indicating the problem and, optionally, some contact information.  When the number of active simultaneous requests drops below the configured amount, then subsequent requests will be processed.

Filter configuration information can be found in [WebXML](WebXML.md).

An example of the response can be found in [ExampleResponse](ExampleResponse.md).


---


<img src='http://www.oclc.org/common/images/logos/oclc/OCLC_TM_V_SM.jpg' />

## License ##
The OCLC Research ipUseThrottleFilter is released under the Apache 2.0 license.

## Contact Information ##
Any questions, comments, suggestions or opinions should be sent to [Ralph LeVan](mailto:levan@oclc.org) (Ralph's Home Page.)<br />
[OCLC Research](http://www.oclc.org/research)<br />
[OCLC](http://www.oclc.org)