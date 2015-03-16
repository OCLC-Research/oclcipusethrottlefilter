#Filter configuration

# Introduction #

How to add the filter to your web.xml file


# Details #

```
  <filter>
    <filter-name>ipUseThrottleFilter</filter-name>
    <filter-class>
      org.oclc.os.ipUseThrottleFilter.ipUseThrottleFilter
      </filter-class>
    <init-param>
      <!-- optional: default value is 3 -->
      <param-name>maxSimultaneousRequests</param-name>
      </param-value>3</param-value>
      </init-param>
    <init-param>
      <!-- optional: if omitted, then no contact information will appear
        in the 403 (Forbidden) response -->
      <param-name>contactInfo</param-name>
      </param-value>bob@example.com</param-value>
      </init-param>
    </filter>

  <filter-mapping>
    <filter-name>ipUseThrottleFilter</filter-name>
    <url-pattern>/*</url-pattern>
    </filter-mapping>
```