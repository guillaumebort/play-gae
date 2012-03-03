package play.modules.gae;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.jsp.ah.adminConsole_jsp;
import org.apache.jsp.ah.backendsBody_jsp;
import org.apache.jsp.ah.backendsFinal_jsp;
import org.apache.jsp.ah.backendsHead_jsp;
import org.apache.jsp.ah.capabilitiesStatusBody_jsp;
import org.apache.jsp.ah.capabilitiesStatusFinal_jsp;
import org.apache.jsp.ah.capabilitiesStatusHead_jsp;
import org.apache.jsp.ah.inboundMailBody_jsp;
import org.apache.jsp.ah.inboundMailFinal_jsp;
import org.apache.jsp.ah.inboundMailHead_jsp;
import org.apache.jsp.ah.taskqueueViewerBody_jsp;
import org.apache.jsp.ah.taskqueueViewerFinal_jsp;
import org.apache.jsp.ah.taskqueueViewerHead_jsp;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerList;
import org.mortbay.jetty.servlet.FilterHolder;
import org.mortbay.jetty.webapp.WebAppContext;

import play.Logger;

import com.google.appengine.api.utils.SystemProperty;
import com.google.appengine.tools.development.ApiProxyLocal;
import com.google.appengine.tools.info.LocalVersionFactory;
import com.google.apphosting.api.ApiProxy;
import com.google.apphosting.utils.servlet.AdminConsoleResourceServlet;
import com.google.apphosting.utils.servlet.CapabilitiesStatusServlet;
import com.google.apphosting.utils.servlet.DatastoreViewerServlet;
import com.google.apphosting.utils.servlet.InboundMailServlet;
import com.google.apphosting.utils.servlet.ServersServlet;
import com.google.apphosting.utils.servlet.TaskQueueViewerServlet;
import com.google.apphosting.utils.servlet.XmppServlet;

public class GAEAdminConsoleServer {

	private static Server server = null;
		
	public static boolean isRunning(){
		return server != null;
	}
	
	public static String getBaseURL(){
		return isRunning() ? "http://localhost:" + server.getConnectors()[0].getPort() : "";
	}
	
	synchronized public static void launch(final PlayDevEnvironment devEnvironment, int port){
		if(isRunning())
			return;
		//sets sdk version
		SystemProperty.version.set(new LocalVersionFactory(Collections.EMPTY_LIST).getVersion().getRelease());
		
		WebAppContext webappcontext = new WebAppContext();
	    webappcontext.setContextPath("/");
	    webappcontext.setWar(devEnvironment.getAppDir().getAbsolutePath());    
	    ApiProxyLocal apriProxyLocal = (ApiProxyLocal) ApiProxy.getDelegate();
        webappcontext.setAttribute("com.google.appengine.devappserver.ApiProxyLocal", apriProxyLocal);
        
        //Mappings for servlets and jsps
        //classes are located in appengine-local-runtime-shared.jar -> packages org.apache.jsp.ah and com.google.apphosting.utils.servlet
        webappcontext.addServlet(DatastoreViewerServlet.class, "/_ah/admin");
        webappcontext.addServlet(adminConsole_jsp.class, "/_ah/adminConsole");
        webappcontext.addServlet(AdminConsoleResourceServlet.class, "/_ah/resources");
        
        webappcontext.addServlet(DatastoreViewerServlet.class, "/_ah/admin/datastore");
        webappcontext.addServlet(org.apache.jsp.ah.datastoreViewerHead_jsp.class, "/_ah/datastoreViewerHead");
        webappcontext.addServlet(org.apache.jsp.ah.datastoreViewerBody_jsp.class, "/_ah/datastoreViewerBody");
        webappcontext.addServlet(org.apache.jsp.ah.datastoreViewerFinal_jsp.class, "/_ah/datastoreViewerFinal");
        webappcontext.addServlet(org.apache.jsp.ah.indexDetailsHead_jsp.class, "/_ah/indexDetailsHead");
        webappcontext.addServlet(org.apache.jsp.ah.indexDetailsBody_jsp.class, "/_ah/indexDetailsBody");
        webappcontext.addServlet(org.apache.jsp.ah.indexDetailsFinal_jsp.class, "/_ah/indexDetailsFinal");
        
        webappcontext.addServlet(TaskQueueViewerServlet.class, "/_ah/admin/taskqueue");
        webappcontext.addServlet(taskqueueViewerHead_jsp.class, "/_ah/taskqueueViewerHead");
        webappcontext.addServlet(taskqueueViewerBody_jsp.class, "/_ah/taskqueueViewerBody");
        webappcontext.addServlet(taskqueueViewerFinal_jsp.class, "/_ah/taskqueueViewerFinal");
        
        webappcontext.addServlet(XmppServlet.class, "/_ah/admin/xmpp");
        webappcontext.addServlet(taskqueueViewerHead_jsp.class, "/_ah/xmppHead");
        webappcontext.addServlet(taskqueueViewerBody_jsp.class, "/_ah/xmppBody");
        webappcontext.addServlet(taskqueueViewerFinal_jsp.class, "/_ah/xmppFinal");
        
        webappcontext.addServlet(InboundMailServlet.class, "/_ah/admin/inboundmail");
        webappcontext.addServlet(inboundMailHead_jsp.class, "/_ah/inboundmailHead");
        webappcontext.addServlet(inboundMailBody_jsp.class, "/_ah/inboundmailBody");
        webappcontext.addServlet(inboundMailFinal_jsp.class, "/_ah/inboundmailFinal");
        
        webappcontext.addServlet(ServersServlet.class, "/_ah/admin/backends");
        webappcontext.addServlet(backendsHead_jsp.class, "/_ah/backendsHead");
        webappcontext.addServlet(backendsBody_jsp.class, "/_ah/backendsBody");
        webappcontext.addServlet(backendsFinal_jsp.class, "/_ah/backendsFinal");
        
        webappcontext.addServlet(CapabilitiesStatusServlet.class, "/_ah/admin/capabilitiesstatus");
        webappcontext.addServlet(capabilitiesStatusHead_jsp.class, "/_ah/capabilitiesstatusHead");
        webappcontext.addServlet(capabilitiesStatusBody_jsp.class, "/_ah/capabilitiesstatusBody");
        webappcontext.addServlet(capabilitiesStatusFinal_jsp.class, "/_ah/capabilitiesstatusFinal");
        		
        webappcontext.addFilter(new FilterHolder(new Filter(){
			@Override
			public void doFilter(ServletRequest req, ServletResponse res,
					FilterChain filterChain) throws IOException, ServletException {
				if(Logger.isDebugEnabled()){
					Logger.debug("Request: " + ((HttpServletRequest) req).getRequestURL().toString());
					for(String name: Collections.list((Enumeration<String>)req.getAttributeNames())){
						Logger.debug(name + "=" + req.getAttribute(name));
					}
				}				
				ApiProxy.setEnvironmentForCurrentThread(devEnvironment);
				filterChain.doFilter(req, res);
			}
			@Override
			public void init(FilterConfig arg0) throws ServletException {}
			@Override
			public void destroy() {}
		}), "/*", Handler.ALL);
        
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] {webappcontext, new DefaultHandler()});
        
        Logger.info("Starting Google App Engine admin console...");
        Server server = new Server(port);
        server.setHandler(handlers);
        try {
        	server.start();
        	GAEAdminConsoleServer.server = server;
		} catch (Exception e) {
			Logger.error(e,"Error starting admin console");
		}
	}
}
