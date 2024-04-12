package org.messenger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.messenger.data.entities.AuthToken;
import org.messenger.data.entities.Message;
import org.messenger.data.entities.Subscription;
import org.messenger.data.entities.User;
import org.messenger.data.implementations.AuthTokenDAO;
import org.messenger.data.implementations.MessageDAO;
import org.messenger.data.implementations.SubscriptionsDAO;
import org.messenger.data.implementations.UsersDAO;
import org.messenger.utils.AuthTokenGenerator;

import java.io.IOException;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;


public class ConnectionManager {

    public interface RequestHandler {void handle(Connection connection, JSONObject dataPacket);}

    static HashMap<String, RequestHandler> handlersMap = new HashMap<>();

    static{


        handlersMap.put("Authorization", (connection, dataPacket) -> {
            try {
                String authType = (String) dataPacket.get("authType");
                JSONObject reply = new JSONObject();
                if(authType.equals("password")) {
                    String username = (String) dataPacket.get("username");
                    UsersDAO usersDAO = new UsersDAO();
                    User user = usersDAO.getByName(username);
                    String token = user == null ? null : AuthTokenGenerator.getToken(user, connection.getIp());
                    String password = (String) dataPacket.get("password");

                    if (user == null || !(user.getPassword().equals(password))) {
                        reply.put("status", "failure");
                        reply.put("desc", "Username or password is incorrect");
                        connection.send(reply);
                        return;
                    }

                    reply.put("status", "OK");
                    reply.put("desc", "Authorization!");
                    if(token == null) token = AuthTokenGenerator.getNewToken(user,connection.getIp());
                    reply.put("token", token);
                    user.setToken(token);
                    connection.auth(user);
                    System.out.println("["+connection.getIp()+":"
                            +connection.getPort()
                            +"] user "+username+" get successfully authorized via password");
                }
                if(authType.equals("authToken")){
                    AuthTokenDAO authTokenDAO = new AuthTokenDAO();
                    String receivedToken = (String) dataPacket.get("token");
                    AuthToken authToken = authTokenDAO.getByToken(receivedToken);
                    if(authToken != null && receivedToken.equals(authToken.getToken())){
                        UsersDAO usersDAO = new UsersDAO();
                        User user = usersDAO.getById(authToken.getUserid());
                        reply.put("status", "OK");
                        reply.put("desc", "Authorization!");
                        user.setToken(receivedToken);
                        connection.auth(user);
                        System.out.println("["+connection.getIp()+":"
                                +connection.getPort()
                                +"] user "+user.getUserName()+" get successfully authorized via token "+receivedToken);
                    }else{
                        reply.put("status","failure");
                        reply.put("desc","token is invalid");
                    }
                }
                connection.send(reply);

            } catch (IOException e) {
                System.out.println("["+connection.getIp()+":"
                        +connection.getPort()
                        +"] Unable to send the reply. Connection is closed");
            }
        });
        handlersMap.put("GetThread", (connection, dataPacket) -> {
            Long threadId = (Long) dataPacket.get("threadId");
            JSONObject reply = new JSONObject();
            reply.put("status","OK");
            UsersDAO usersDAO = new UsersDAO();
            if(threadId != null){
                MessageDAO messageDAO = new MessageDAO();
                Message parentMessage = messageDAO.getByMessageId(threadId);
                if(parentMessage != null) {
                    JSONObject parentMessageJSON = messageToJSON(parentMessage);
                    reply.put("parentMessage", parentMessageJSON);
                    LinkedList<Message> messages = messageDAO.getByParentMessageIdPaginate(threadId, 0L, MessageDAO.PagingMode.nextMessages,999999999);
                    JSONArray jsonArray = new JSONArray();
                    for (Message message : messages) {
                        JSONObject messageJSON = messageToJSON(message);
                        jsonArray.add(messageJSON);
                    }
                    reply.put("messages", jsonArray);
                }
            }
            try {
                connection.send(reply);
            } catch (IOException e) {
                System.out.println("["+connection.getIp()+":"
                        +connection.getPort()
                        +"] Unable to send the reply. Connection is closed");
            }
        });
        handlersMap.put("GetLastMessage", (connection, dataPacket) -> {
           //JSONObject reply = new JSONObject();
           Long threadId = (Long) dataPacket.get("threadId");
           MessageDAO messageDAO = new MessageDAO();
           LinkedList<Message> messages = messageDAO.getByParentMessageIdPaginate(threadId, 999999999999999L, MessageDAO.PagingMode.prevMessages,1);
           if(messages.isEmpty()) {
               messages = new LinkedList<>();
               Message firstMessage = messageDAO.getByMessageId(threadId);
               if(firstMessage != null)
                    messages.add(messageDAO.getByMessageId(threadId));
           }
            JSONObject reply;
           if(messages.isEmpty()){
               reply = new JSONObject();
               reply.put("status","failed");
               reply.put("desc","no such thread");
           }else {
               Message message = messages.getFirst();
               reply = messageToJSON(message);
               reply.put("status", "OK");
           }
           try {
               connection.send(reply);
           } catch (IOException e) {
               System.out.println("["+connection.getIp()+":"
                       +connection.getPort()
                       +"] Unable to send the reply. Connection is closed");
           }
        });

        handlersMap.put("GetSubscriptions", (connection, dataPacket) ->{
            JSONObject reply = new JSONObject();
            if(connection.isAuthorized()){
                reply.put("status","OK");
                SubscriptionsDAO subscriptionsDAO = new SubscriptionsDAO();
                MessageDAO messageDAO = new MessageDAO();
                LinkedList<Subscription> subscriptions = subscriptionsDAO.getUserSubscriptions(connection.getUser().getId());
                JSONArray subscriptionsArray = new JSONArray();
                for(Subscription sub : subscriptions){
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("topic",sub.getParentMessageId());
                    jsonObject.put("lastReadTime",sub.getLastReadTime().getTime());
                    jsonObject.put("firstMessage",messageDAO.getByMessageId(sub.getParentMessageId()).getContent());
                    jsonObject.put("isNotification",sub.isNotification());
                    subscriptionsArray.add(jsonObject);
                }
                reply.put("subscriptions",subscriptionsArray);
            }else{
                reply.put("status","failure");
                reply.put("desc","user is not authorized");
            }
            try {
                connection.send(reply);
            } catch (IOException e) {
                System.out.println("["+connection.getIp()+":"
                        +connection.getPort()
                        +"] Unable to send the reply. Connection is closed");
            }
        });
        handlersMap.put("Subscribe", (connection, dataPacket) -> {
            JSONObject reply = new JSONObject();
            if(connection.isAuthorized()) {
                Long threadId = (Long) dataPacket.get("threadId");
                SubscriptionsDAO subscriptionsDAO = new SubscriptionsDAO();
                LinkedList<Subscription> subscriptions = subscriptionsDAO.getUserSubscriptions(connection.getUser().getId());
                boolean exists = false;
                for (Subscription sub : subscriptions) {
                    if (sub.getParentMessageId() == threadId) {
                        reply.put("status", "failure");
                        reply.put("desc", "already subscribed");
                        exists = true;
                    }
                }
                if (!exists) {
                    if (threadId == null) {
                        reply.put("status", "failure");
                        reply.put("desc", "incorrect threadId");
                    } else {
                        MessageDAO messageDAO = new MessageDAO();
                        if(messageDAO.getByMessageId(threadId) == null){
                            reply.put("status", "failure");
                            reply.put("desc", "thread doesn't exists");
                        }else {
                            Subscription subscription = new Subscription();
                            subscription.setLastReadTime(new Date().getTime());
                            subscription.setParentMessageId(threadId);
                            subscription.setUserId(connection.getUser().getId());
                            subscriptionsDAO.create(subscription);
                            reply.put("status", "OK");
                            reply.put("desc", "subscription has been created");
                            NotificationManager.updateSubscriptions(connection.getUser());
                        }
                    }
                }

            }else{
                reply.put("status","failure");
                reply.put("desc","user isn't authorized");
            }
            try {
                connection.send(reply);
            } catch (IOException e) {
                System.out.println("["+connection.getIp()+":"
                        +connection.getPort()
                        +"] Unable to send the reply. Connection is closed");
            }
        });
        handlersMap.put("Unsubscribe", (connection, dataPacket) -> {
            JSONObject reply = new JSONObject();
            if(connection.isAuthorized()) {
                Long threadId = (Long) dataPacket.get("threadId");
                SubscriptionsDAO subscriptionsDAO = new SubscriptionsDAO();
                LinkedList<Subscription> subscriptions = subscriptionsDAO.getUserSubscriptions(connection.getUser().getId());
                if (threadId != null) {
                    boolean exists = false;
                    for (Subscription sub : subscriptions) {
                        if (sub.getParentMessageId() == threadId) {
                            subscriptionsDAO.delete(sub);
                            reply.put("status", "OK");
                            reply.put("desc", "unsubscribed of" + threadId);
                            exists = true;
                            NotificationManager.updateSubscriptions(connection.getUser());
                        }
                    }
                    if (!exists) {
                        if (threadId == null) {
                            reply.put("status", "failure");
                            reply.put("desc", "no such subscription");
                        }
                    } else {
                        reply.put("status", "failure");
                        reply.put("desc", "incorrect threadId");
                    }
                }
            }else{
                reply.put("status", "failure");
                reply.put("desc", "you are not authorized");
            }
            try {
                connection.send(reply);
            } catch (IOException e) {
                System.out.println("["+connection.getIp()+":"
                        +connection.getPort()
                        +"] Unable to send the reply. Connection is closed");
            }
        });

        handlersMap.put("SendMessage", (connection, dataPacket) -> {
            JSONObject reply = new JSONObject();
            if(connection.isAuthorized()) {
                Long threadId = (Long) dataPacket.get("threadId");
                MessageDAO messageDAO = new MessageDAO();
                Message threadMessage = messageDAO.getByMessageId(threadId);
                if(threadMessage != null){
                    Message message = new Message();
                    message.setAuthorId(connection.getUser().getId());
                    message.setContent((String)dataPacket.get("content"));
                    message.setDatetime(new Timestamp(new Date().getTime()));
                    message.setParentMessageId(threadId);
                    messageDAO.create(message);
                    SubscriptionsDAO subscriptionsDAO = new SubscriptionsDAO();
                    /*NotificationManager.notificationConnections.forEach((user, notificationConnection) -> {
                        if (user.getId() == connection.getUser().getId()) return;
                        LinkedList<Subscription> subscriptions = subscriptionsDAO.getUserSubscriptions(user.getId());
                        for (Subscription sub : subscriptions) {
                            if (sub.getParentMessageId() == threadId) {
                                JSONObject notification = new JSONObject();
                                notification.put("topic",sub.getParentMessageId());
                                notification.put("firstMessage",messageDAO.getByParentMessageIdPaginate(sub.getParentMessageId(), 0L, MessageDAO.PagingMode.nextMessages,1).getFirst().getContent());
                                NotificationManager.notifyUser(user, notification);
                            }
                        }
                    });*/
                    JSONObject notification = new JSONObject();
                    notification.put("threadId",threadId);
                    notification.put("firstMessage",messageDAO.getByParentMessageIdPaginate(threadId, 0L, MessageDAO.PagingMode.nextMessages,1).getFirst().getContent());
                    NotificationManager.notifyInThread(threadId,notification);

                    reply.put("status","OK");
                    reply.put("desc","Message sent successfully");
                }else{
                    reply.put("status","failure");
                    reply.put("desc","no such thread exists");
                }
            }
            try {
                connection.send(reply);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        handlersMap.put("Read", ((connection, dataPacket) -> {
            JSONObject reply = new JSONObject();
            if(connection.isAuthorized()){
                Long threadId = (Long) dataPacket.get("threadId");
                if(threadId != null) {
                    SubscriptionsDAO subscriptionsDAO = new SubscriptionsDAO();
                    LinkedList<Subscription> subscriptions = subscriptionsDAO.getUserSubscriptions(connection.getUser().getId());
                    boolean beenRead = false;
                    for (Subscription sub : subscriptions) {
                        if (sub.getParentMessageId() == threadId) {
                            if(sub.isNotification()) {
                                subscriptionsDAO.delete(sub);
                            }else {
                                sub.setLastReadTime(new Date().getTime());
                                subscriptionsDAO.update(sub);
                            }
                            beenRead = true;
                            break;
                        }
                    }
                    if (beenRead) {
                        reply.put("status", "OK");
                        reply.put("desc", "Topic has been read");
                    } else {
                        reply.put("status", "failure");
                        reply.put("desc", "No such topic in subscriptions");
                    }
                }else{
                    reply.put("status","failure");
                    reply.put("desc","Incorrect threadId provided");
                }
            }else{
                reply.put("status","failure");
                reply.put("desc","Can't identify user");
            }

            try{
                connection.send(reply);
            } catch (IOException e) {
                System.out.println("[" + connection.getIp() + ":"
                        + connection.getPort()
                        + "] Unable to send the reply. Connection is closed");
            }
        }));
    }

    private ConnectionManager(){}; //Unable to access


    static void connect(Socket socket){
        Connection connection = new Connection(socket);
        connection.start();
        System.out.println("["+connection.socket().getInetAddress().getHostAddress()+":"+connection.socket().getPort()+"] Connection opened");
    }

    static void handleConnection(Connection connection, Boolean isRunning){
    Object request=null;
        while (isRunning){
            try {
                request = connection.getRequest();
                JSONObject dataPacket = (JSONObject) request;
                String requestDescription = (String) dataPacket.get("requestDescription");
                RequestHandler requestHandler = handlersMap.get(requestDescription);
                if(requestHandler == null){
                    JSONObject reply = new JSONObject();
                    reply.put("status","failure");
                    reply.put("desc","no such endpoint");
                    connection.send(reply);
                }else{
                    requestHandler.handle(connection,dataPacket);
                }
            } catch (IOException e) {
                System.out.println("["+connection.getIp()+":"
                        +connection.getPort()
                        +"] Stream was closed or didn't opened");
                break;
            }catch (ClassCastException | ClassNotFoundException e){
                System.out.println("["+connection.getIp()+":"
                        +connection.getPort()
                        +"] Incorrect data format: "
                        +request.getClass()+": "+request + e.getMessage());
            }
        }
        System.out.println("["+connection.getIp()+":"
                +connection.getPort()
                +"] Connection closed");
    }

    private static JSONObject messageToJSON(Message message){
        UsersDAO usersDAO = new UsersDAO();
        JSONObject messageJSON = new JSONObject();
        messageJSON.put("id",message.getMessageId());
        Long parentMessageId = message.getParentMessageId();
        messageJSON.put("parentMessage",parentMessageId == null ? -1L : parentMessageId);
        messageJSON.put("author", usersDAO.getById(message.getAuthorId()).getUserName());
        messageJSON.put("content",message.getContent());
        messageJSON.put("dateTime",message.getDatetime().getTime());
        messageJSON.put("quotes",message.getQuotes());
        return messageJSON;
    }
}
