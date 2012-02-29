package play.modules.gae;

import com.google.appengine.tools.development.ApiProxyLocal;
import com.google.appengine.tools.development.ApiProxyLocalFactory;
import com.google.appengine.tools.development.LocalServerEnvironment;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalTaskQueueTestConfig;
import com.google.apphosting.api.ApiProxy;
import com.google.apphosting.api.ApiProxy.Environment;
import play.Play;
import play.mvc.Http;
import play.mvc.Scope.Session;
import play.server.Server;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class PlayDevEnvironment implements Environment, LocalServerEnvironment {

    public static PlayDevEnvironment create() {
        PlayDevEnvironment instance = new PlayDevEnvironment();
        ApiProxyLocalFactory factory = new ApiProxyLocalFactory();
        ApiProxyLocal proxy = factory.create(instance);
        ApiProxy.setDelegate(proxy);
        
        //Config datastore api. Save datastore file in tmp/
		LocalDatastoreServiceTestConfig datastoreConfig = new LocalDatastoreServiceTestConfig();
		datastoreConfig.setNoStorage(false);
		datastoreConfig.setBackingStoreLocation(Play.applicationPath + "/tmp/datastore");
		boolean enableMasterSlave = Boolean.parseBoolean(Play.configuration.getProperty("gae.datastore.enableMasterSlave", "false"));
	    if(!enableMasterSlave){ //activate HRD
	    	float unappliedJobPct = Float.parseFloat(Play.configuration.getProperty("gae.datastore.hrd.unappliedJobPct","50"));
	    	datastoreConfig.setDefaultHighRepJobPolicyUnappliedJobPercentage(unappliedJobPct);
	    }
		datastoreConfig.setUp();
	    
	    // Use local implementation for deferred queues
		LocalTaskQueueTestConfig taskQueueConfig = new LocalTaskQueueTestConfig();
		taskQueueConfig.setDisableAutoTaskExecution(false);
		taskQueueConfig.setShouldCopyApiProxyEnvironment(true);
		taskQueueConfig.setCallbackClass(LocalTaskQueueTestConfig.DeferredTaskCallback.class);
		taskQueueConfig.setUp();
        
        return instance;
    }

    @Override
    public String getAppId() {
        return Play.applicationPath.getName();
    }

    @Override
    public String getVersionId() {
        return "1.0";
    }

    @Override
    public String getEmail() {
        if(Session.current() != null) {
            return Session.current().get("__GAE_EMAIL");
        } else {
            return "no-session@gmail.com";
        }
    }

    @Override
    public boolean isLoggedIn() {
        return Session.current() != null && Session.current().contains("__GAE_EMAIL");
    }

    @Override
    public boolean isAdmin() {
        return Session.current() != null && Session.current().contains("__GAE_ISADMIN") && Session.current().get("__GAE_ISADMIN").equals("true");
    }

    @Override
    public String getAuthDomain() {
        return "gmail.com";
    }

    @Override
    public String getRequestNamespace() {
        return "";
    }

    public String getDefaultNamespace() {
        return "";
    }

    public void setDefaultNamespace(String ns) {
    }

    @Override
    public Map<String, Object> getAttributes() {
        return new HashMap<String, Object>();
    }

    @Override
    public void waitForServerToStart() throws InterruptedException {
    }

    @Override
    public int getPort() {
        return Server.httpPort;
    }

    @Override
    public File getAppDir() {
        return new File(Play.applicationPath, "war");
    }

    @Override
    public String getAddress() {
        return "localhost";
    }

	@Override
	public boolean enforceApiDeadlines() {
		return false;
	}
    
	@Override
	public boolean simulateProductionLatencies() {
		return false;
	}
	
	@Override
	public String getHostName() {
		return getBaseUrl();
	}

	// code stolen from Play core as this function is protected in Router
	// Gets baseUrl from current request or application.baseUrl in application.conf
    protected static String getBaseUrl() {
        if (Http.Request.current() == null) {
            // No current request is present - must get baseUrl from config
            String appBaseUrl = Play.configuration.getProperty("application.baseUrl", "application.baseUrl");
            if (appBaseUrl.endsWith("/")) {
                // remove the trailing slash
                appBaseUrl = appBaseUrl.substring(0, appBaseUrl.length()-1);
            }
            return appBaseUrl;

        } else {
            return Http.Request.current().getBase();
        }
    }


}

