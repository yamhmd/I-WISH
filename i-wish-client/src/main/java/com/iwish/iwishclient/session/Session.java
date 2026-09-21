package com.iwish.iwishclient.session;

import com.iwish.iwishclient.model.User;
import com.iwish.iwishclient.network.ApiClient;

public class Session {
    private static final Session INSTANCE = new Session();

    private User currentUser;
    private ApiClient api;

    private Session() {}

    public static Session get() {
        return INSTANCE;
    }

    public User getCurrentUser() { return currentUser; }
    public void setCurrentUser(User user) { this.currentUser = user; }

    public ApiClient getApi() { return api; }
    public void setApi(ApiClient api) { this.api = api; }

    public void logout() { currentUser = null; }
}
