package play.modules.gae;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import play.mvc.Http.Request;
import play.mvc.Router;
import play.mvc.results.Redirect;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

public class GAE {

    public static DatastoreService getDatastore() {
        return DatastoreServiceFactory.getDatastoreService();
    }

    public static UserService getUserService() {
        return UserServiceFactory.getUserService();
    }

    public static URLFetchService getURLFetchService() {
        return URLFetchServiceFactory.getURLFetchService();
    }

    /**
     * Authenticate the user against the App Engine and redirect to the given
     * action and parameters if the login is successful
     * 
     * @param returnAction
     *            the action where the user will be redirected after the login
     * @param returnParams
     *            the action parameters
     */
    public static void login(String returnAction,
            Map<String, Object> returnParams) {

        if (returnAction == null || returnAction.isEmpty())
            throw new IllegalArgumentException("Empty action");

        if (returnParams == null || returnParams.isEmpty())
            returnParams = new HashMap<String, Object>();

        String returnURL = Router.getFullUrl(returnAction, returnParams);
        String url = getUserService().createLoginURL(returnURL);
        throw new Redirect(url);
    }

    /**
     * Authenticate the user against App Engine and redirect to the given action
     * if the login is successful
     * 
     * @param returnAction
     *            the action where the user will be redirected after the login
     */
    public static void login(String returnAction) {
        login(returnAction, null);
    }

    /**
     * Authenticate the user against App Engine if the login is successful
     */
    public static void login() {

        Map<String, Object> params = new HashMap<String, Object>();
        for (Entry<String, String> param : Request.current().routeArgs
                .entrySet())
            params.put(param.getKey(), param.getValue());

        login(Request.current().action, params);
    }

    public static User getUser() {
        return getUserService().getCurrentUser();
    }

    public static boolean isLoggedIn() {
        return getUserService().isUserLoggedIn();
    }

    public static boolean isAdmin() {
        return getUserService().isUserAdmin();
    }

    public static void logout(String returnAction) {
        String returnURL = Router.getFullUrl(returnAction);
        String url = getUserService().createLogoutURL(returnURL);
        throw new Redirect(url);
    }

    public static void logout() {
        logout(Request.current().action);
    }

}
