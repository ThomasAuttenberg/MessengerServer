package org.messenger;

import java.lang.reflect.Method;
import java.util.HashMap;

public class RequestManager {

    enum RequestType{
        GetThread,
        Authorization
    }

    static HashMap<RequestType, RequestHandler> handlersMap = new HashMap<>();

    static{
        handlersMap.put(RequestType.GetThread, new RequestHandler() {
            @Override
            public String handle() {
                return null;
            }
        });
    }

    static void handle(){

    }
}
