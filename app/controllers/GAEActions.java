package controllers;

import play.Play;
import play.Play.Mode;
import play.modules.gae.GAEAdminConsoleServer;
import play.modules.gae.GAEPlugin;
import play.modules.gae.PlayDevEnvironment;
import play.mvc.*;

public class GAEActions extends Controller {

    public static void login() {
        String url = params.get("continue");
        render(url);
    }

    public static void doLogin(String email, String url, boolean isAdmin) {
        if(email!= null && !email.trim().equals("")) {
            session.put("__GAE_EMAIL", email);
            session.put("__GAE_ISADMIN", isAdmin);
        }
        redirect(url);
    }

    public static void logout() {
        String url = params.get("continue");
        session.clear();
        redirect(url);
    }
    
    public static void adminConsole(){
    	if(Play.mode == Mode.DEV){
    		if(!GAEAdminConsoleServer.isRunning()){
    			PlayDevEnvironment devEnvironment = ((GAEPlugin) Play.pluginCollection.getPluginInstance(GAEPlugin.class)).devEnvironment;
    			int port = Integer.parseInt(Play.configuration.getProperty("gae.adminConsole.port", "9050"));
    			GAEAdminConsoleServer.launch(devEnvironment, port);
    		}
    		renderArgs.put("baseUrl", GAEAdminConsoleServer.getBaseURL());
    		render();
    	}
    }

}