package play.modules.gae;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
     * @param returnAction
     *            the action where the user will be redirected after the login
     * @param returnParams
     *            the action parameters
     * @param federatedIdentity
     *            federated identity string which is to be asserted for users
     *            who are authenticated using a non-Google ID (e.g., OpenID). In
     *            order to use federated logins this feature must be enabled for
     *            the application. Otherwise, this should be null.
     */
    public static void login(String returnAction, Map<String, Object> returnParams, String federatedIdentity) {
        login(returnAction, returnParams, federatedIdentity, new HashSet());
    }

    /**
     * @param returnAction
     *            the action where the user will be redirected after the login
     * @param federatedIdentity
     *            federated identity string which is to be asserted for users
     *            who are authenticated using a non-Google ID (e.g., OpenID). In
     *            order to use federated logins this feature must be enabled for
     *            the application. Otherwise, this should be null.
     */
    public static void login(String returnAction, String federatedIdentity) {
        login(returnAction, null, federatedIdentity, new HashSet());
    }

    /**
     * @param returnAction
     *            the action where the user will be redirected after the login
     * @param federatedIdentity
     *            federated identity string which is to be asserted for users
     *            who are authenticated using a non-Google ID (e.g., OpenID). In
     *            order to use federated logins this feature must be enabled for
     *            the application. Otherwise, this should be null.
     * @param attributesRequest
     *            additional attributions requested for this login, IDP may not
     *            may not support these attributes
     */
    public static void login(String returnAction, String federatedIdentity, Set<String> attributesRequest) {
        login(returnAction, null, federatedIdentity, attributesRequest);
    }

    /**
     * @param returnAction
     *            the action where the user will be redirected after the login
     * @param returnParams
     *            the action parameters
     * @param federatedIdentity
     *            federated identity string which is to be asserted for users
     *            who are authenticated using a non-Google ID (e.g., OpenID). In
     *            order to use federated logins this feature must be enabled for
     *            the application. Otherwise, this should be null.
     * @param attributesRequest
     *            additional attributions requested for this login, IDP may not
     *            may not support these attributes
     */
    public static void login(String returnAction, Map<String, Object> returnParams, String federatedIdentity,
            Set<String> attributesRequest) {

        if (returnAction == null || returnAction.isEmpty())
            throw new IllegalArgumentException("Empty action");

        if (returnParams == null || returnParams.isEmpty())
            returnParams = new HashMap<String, Object>();

        String returnURL = Router.getFullUrl(returnAction, returnParams);
        String url = null;
        if (federatedIdentity != null) {
            url = getUserService().createLoginURL(returnURL, null, federatedIdentity, attributesRequest);
        } else {
            url = getUserService().createLoginURL(returnURL);
        }
        throw new Redirect(url);
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
    public static void login(String returnAction, Map<String, Object> returnParams) {
        login(returnAction, returnParams, null, null);
    }

    /**
     * Authenticate the user against App Engine and redirect to the given action
     * if the login is successful
     * 
     * @param returnAction
     *            the action where the user will be redirected after the login
     */
    public static void login(String returnAction) {
        login(returnAction, null, null, null);
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
