/*
   Copyright 2012 OCLC Online Computer Library Center, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 */

package org.oclc.os.ipUseThrottleFilter;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author levan
 */
public class ipUseThrottleFilter implements Filter {
    static Log log=LogFactory.getLog(ipUseThrottleFilter.class);
    static final ConcurrentHashMap<String, Integer> simultaneousRequests=new ConcurrentHashMap<String, Integer>();
    static final HashMap<String, Integer> totalRequests=new HashMap<String, Integer>();
    static int maxSimultaneousRequests=3, nextReportingHour;
    static String contactInfo=null;

    @Override
    public void init(FilterConfig fc) throws ServletException {
        Calendar c=new GregorianCalendar();
        nextReportingHour=c.get(Calendar.HOUR_OF_DAY)+1;
        String t=fc.getInitParameter("maxSimultaneousRequests");
        if(t!=null)
        try {
            maxSimultaneousRequests=Integer.parseInt(t);
        }
        catch(Exception e) {
            log.error("Bad value for parameter 'maxSimultaneousRequests': '"+t+"'");
            log.error("Using the default value of 3 instead");
        }
        contactInfo=fc.getInitParameter("contactInfo");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String addr=request.getRemoteAddr();
        int count;
        synchronized(simultaneousRequests) {
            Integer icount=simultaneousRequests.get(addr);
            if(icount!=null)
                count=icount.intValue();
            else
                count=0;

            if(count>maxSimultaneousRequests) {
                log.error("IP addr "+addr+" has exceeded "+maxSimultaneousRequests+" simultaneous requests!");
                ((HttpServletResponse)response).setStatus(HttpURLConnection.HTTP_FORBIDDEN);
                response.setContentType("text/html");
                PrintWriter writer = response.getWriter();
                writer.println( "<html><body><h1>Forbidden</h1>" );
                writer.println( "You have exceeded the maximum simultaneous request value of "+maxSimultaneousRequests);
                writer.println("<p>This message and your IP address have been logged and reported</p>");
                if(contactInfo!=null)
                    writer.println("<p>Contact "+contactInfo+" for more information</p>");
                writer.println("</body></html>");
                writer.close();
                return;
            }
            simultaneousRequests.put(addr, count+1);
            icount=totalRequests.get(addr);
            if(icount!=null)
                count=icount.intValue();
            else
                count=0;
            totalRequests.put(addr, count+1);
        }

        HttpServletResponseWrapper wrapper = new HttpServletResponseWrapper((HttpServletResponse)response);
        chain.doFilter(request, wrapper);

        synchronized(simultaneousRequests) {
            count=simultaneousRequests.get(addr);
            simultaneousRequests.put(addr, count-1);
        }

        Calendar c=new GregorianCalendar();
        int hour=c.get(Calendar.HOUR_OF_DAY);
        if(hour==0 && nextReportingHour==24) { // new day!
            nextReportingHour=0;
        }

        if(hour>=nextReportingHour) { // generate the hourly report
            nextReportingHour=hour+1;

            if(log.isInfoEnabled()) {
                HashMap<String, Integer> map = new LinkedHashMap();
                List<String> yourMapKeys = new ArrayList(totalRequests.keySet());
                List<Integer> yourMapValues = new ArrayList(totalRequests.values());
                TreeSet<Integer> sortedSet = new TreeSet(yourMapValues);
                Integer[] sortedArray = sortedSet.toArray(new Integer[0]);
                int size = sortedArray.length;

                for (int i=0; i<size; i++)
                    map.put(yourMapKeys.get(yourMapValues.indexOf(sortedArray[i])),
                        sortedArray[i]);
                Iterator<String> it=map.keySet().iterator();
                String key;
                log.info("top 10 users");
                for(int i=0; i<10 && it.hasNext(); i++) {
                    key=it.next();
                    log.info("    "+key+" : "+map.get(key));
                }
            }
        }
    }

    @Override
    public void destroy() {
    }
    
}
