package play.modules.gae;

import com.google.appengine.api.datastore.dev.LocalDatastoreService;
import com.google.appengine.tools.development.ApiProxyLocal;
import com.google.appengine.tools.development.ApiProxyLocalFactory;
import com.google.appengine.tools.development.LocalServerEnvironment;
import com.google.apphosting.api.ApiProxy;
import com.google.apphosting.api.ApiProxy.Environment;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import play.Play;
import play.mvc.Scope.Session;
import play.server.Server;

public class PlayDevEnvironment implements Environment, LocalServerEnvironment {

    public static PlayDevEnvironment create() {
        PlayDevEnvironment instance = new PlayDevEnvironment();
        ApiProxyLocalFactory factory = new ApiProxyLocalFactory();
        ApiProxyLocal proxy = factory.create(instance);
        proxy.setProperty(
                LocalDatastoreService.BACKING_STORE_PROPERTY,
                Play.getFile("tmp/datastore").getAbsolutePath());
        ApiProxy.setDelegate(proxy);
        return instance;
    }

    public String getAppId() {
        return Play.applicationPath.getName();
    }

    public String getVersionId() {
        return "1.0";
    }

    public String getEmail() {
        return Session.current().get("__GAE_EMAIL");
    }

    public boolean isLoggedIn() {
        return Session.current().contains("__GAE_EMAIL");
    }

    public boolean isAdmin() {
        return Session.current().contains("__GAE_ISADMIN") && Session.current().get("__GAE_ISADMIN").equals("true");
    }

    public String getAuthDomain() {
        return "gmail.com";
    }

    public String getRequestNamespace() {
        return "";
    }

    public String getDefaultNamespace() {
        return "";
    }

    public void setDefaultNamespace(String ns) {
    }

    public Map<String, Object> getAttributes() {
        return new HashMap<String, Object>();
    }

    public void waitForServerToStart() throws InterruptedException {
    }

    public int getPort() {
        return Server.httpPort;
    }

    public File getAppDir() {
        return new File(Play.applicationPath, "war");
    }

    public String getAddress() {
        return "localhost";
    }

}

