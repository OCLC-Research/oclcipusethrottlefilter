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
    static int maxSimultaneousRequests=3, maxTotalSimultaneousRequests=10, nextReportingHour;
    static int totalSimultaneousRequests=0;
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

        t=fc.getInitParameter("maxTotalSimultaneousRequests");
        if(t!=null)
        try {
            maxTotalSimultaneousRequests=Integer.parseInt(t);
        }
        catch(Exception e) {
            log.error("Bad value for parameter 'maxTotalSimultaneousRequests': '"+t+"'");
            log.error("Using the default value of 10 instead");
        }

        contactInfo=fc.getInitParameter("contactInfo");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String longAddr=request.getRemoteAddr();
        String shortAddr=longAddr.substring(0, longAddr.lastIndexOf('.')); // trim off 4th number group
        // that lets us spot requests from clusters
        int count;
        synchronized(simultaneousRequests) {
            if(totalSimultaneousRequests>=maxTotalSimultaneousRequests) {
                log.error("This system has exceeded the maxTotalSimultaneousRequests limit of "+maxTotalSimultaneousRequests);
                ((HttpServletResponse)response).setStatus(HttpURLConnection.HTTP_UNAVAILABLE);
                response.setContentType("text/html");
                PrintWriter writer = response.getWriter();
                writer.println( "<html><body><h1>Service Temporarily Unavailable</h1>" );
                writer.println( "The system is experiencing a severe load and is temporarily unable to accept new requests");
                if(contactInfo!=null)
                    writer.println("<p>Contact "+contactInfo+" for more information</p>");
                writer.println("</body></html>");
                writer.close();
                return;
            }
            Integer icount=simultaneousRequests.get(shortAddr);
            if(icount!=null)
                count=icount.intValue();
            else
                count=0;

            if(count>maxSimultaneousRequests) {
                log.error("IP addr "+shortAddr+".* has exceeded "+maxSimultaneousRequests+" simultaneous requests!");
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
            simultaneousRequests.put(shortAddr, count+1);
            icount=totalRequests.get(shortAddr);
            if(icount!=null)
                count=icount.intValue();
            else
                count=0;
            totalRequests.put(shortAddr, count+1);
            totalSimultaneousRequests++;
        }

        try {
            HttpServletResponseWrapper wrapper = new HttpServletResponseWrapper((HttpServletResponse)response);
            chain.doFilter(request, wrapper);
        }
        finally {
            synchronized(simultaneousRequests) {
                totalSimultaneousRequests--;
                count=simultaneousRequests.get(shortAddr);
                if(count==1) // prune them from the table
                    simultaneousRequests.remove(shortAddr);
                else
                    simultaneousRequests.put(shortAddr, count-1);
            }
        }

        Calendar c=new GregorianCalendar();
        int hour=c.get(Calendar.HOUR_OF_DAY);
        if(hour==0 && nextReportingHour==24) { // new day!
            // you could reset your daily limits table here
            nextReportingHour=0;
        }

        if(hour>=nextReportingHour) { // generate the hourly report
            // you could reset your hourly limits table here
            nextReportingHour=hour+1;

            if(log.isInfoEnabled()) {
                HashMap<String, Integer> map = new LinkedHashMap();
                List<String> yourMapKeys = new ArrayList(totalRequests.keySet());
                List<Integer> yourMapValues = new ArrayList(totalRequests.values());
                TreeSet<Integer> sortedSet = new TreeSet(yourMapValues);
                Integer[] sortedArray = sortedSet.descendingSet().toArray(new Integer[0]);
                int size = sortedArray.length;

                for (int i=0; i<size; i++)
                    map.put(yourMapKeys.get(yourMapValues.indexOf(sortedArray[i])),
                        sortedArray[i]);
                Iterator<String> it=map.keySet().iterator();
                String key;
                StringBuilder sb=new StringBuilder("Top 10 users in the last hour");
                for(int i=0; i<10 && it.hasNext(); i++) {
                    key=it.next();
                    sb.append("\n    ").append(key).append(" : ").append(map.get(key));
                }
                log.info(sb);
            }
        }
    }

    @Override
    public void destroy() {
    }
    
}
