package play.modules.gae;

import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.cache.Cache;
import play.jobs.JobsPlugin;
import play.libs.IO;
import play.libs.Mail;
import play.mvc.Router;

import javax.mail.Session;
import java.io.File;
import java.util.*;

import com.google.apphosting.api.ApiProxy;

public class GAEPlugin extends PlayPlugin {

    public PlayDevEnvironment devEnvironment = null;
    public boolean prodGAE;

    @Override
    public void onLoad() {
        // Remove Jobs from plugin list
        for (ListIterator<PlayPlugin> it = Play.plugins.listIterator(); it.hasNext();) {
            PlayPlugin p = it.next();
            if (p instanceof JobsPlugin) {
                it.remove();
            }
        }
        // Create a fake development environment if not run in the Google SDK
        if (ApiProxy.getCurrentEnvironment() == null) {
            Logger.warn("");
            Logger.warn("Google App Engine module");
            Logger.warn("~~~~~~~~~~~~~~~~~~~~~~~");
            Logger.warn("No Google App Engine environment found. Setting up a development environement");
            devEnvironment = PlayDevEnvironment.create();
            System.setProperty("appengine.orm.disable.duplicate.emf.exception", "yes");
            File warExt = Play.getFile("war");
            if (!warExt.exists()) {
                warExt.mkdir();
            }
            File webInf = Play.getFile("war/WEB-INF");
            if (!webInf.exists()) {
                webInf.mkdir();
            }
            File xml = Play.getFile("war/WEB-INF/appengine-web.xml");
            try {
                if (!xml.exists()) {
                    IO.writeContent("<appengine-web-app xmlns=\"http://appengine.google.com/ns/1.0\">\n" +
                            "\t<application><!-- Replace this with your application id from http://appengine.google.com --></application>\n" +
                            "\t<version>1</version>\n" +
                            "</appengine-web-app>\n", xml);
                }
                if (IO.readContentAsString(xml).contains("<!-- Replace this with your application id from http://appengine.google.com -->")) {
                    Logger.warn("Don't forget to define your GAE application id in the 'war/WEB-INF/appengine-web.xml' file");
                }
            } catch (Exception e) {
                Logger.error(e, "Cannot init GAE files");
            }
            Logger.warn("");
        } else {
            // Force to PROD mode when hosted on production GAE
            Play.mode = Play.Mode.PROD;
            prodGAE = true;
        }
    }

    @Override
    public void onRoutesLoaded() {
        Router.addRoute("GET", "/_ah/login", "GAEActions.login");
        Router.addRoute("POST", "/_ah/login", "GAEActions.doLogin");
        Router.addRoute("GET", "/_ah/logout", "GAEActions.logout");
    }

    @Override
    public void onApplicationStart() {
        // Wrap the GAE cache
        if (devEnvironment == null) {
            Cache.forcedCacheImpl = new GAECache(); 
        }

        // Provide the correct JavaMail session
        Mail.session = Session.getDefaultInstance(new Properties(), null);
        Mail.asynchronousSend = false;
    }
    
    @Override
    public void beforeInvocation() {
        // Set the current development environment if needed
        if (devEnvironment != null) {
            ApiProxy.setEnvironmentForCurrentThread(new PlayDevEnvironment());
        }
    }

    @Override
    public void onConfigurationRead() {
        if (devEnvironment == null) {
            Play.configuration.remove("smtp.mock");
            Play.configuration.setProperty("application.log", "DEBUG");
        }
        Play.configuration.setProperty("webservice", "urlfetch");
        Play.configuration.setProperty("upload.threshold", Integer.MAX_VALUE + "");
    }
}
