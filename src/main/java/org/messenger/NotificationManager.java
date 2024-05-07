package org.messenger;

import org.json.simple.JSONObject;
import org.messenger.data.entities.AuthToken;
import org.messenger.data.entities.Subscription;
import org.messenger.data.entities.User;
import org.messenger.data.implementations.AuthTokenDAO;
import org.messenger.data.implementations.MessageDAO;
import org.messenger.data.implementations.SubscriptionsDAO;
import org.messenger.data.implementations.UsersDAO;

import java.io.IOException;
import java.net.Socket;
import java.util.*;

public class NotificationManager {

    //уведомление от сервака:
    // Подписка: topicId, время последнего чтения

    //

    static final HashMap<User, NotificationConnection> notificationConnections = new HashMap<>(); // ключ: пользователь, значение: содинение с пользователем
    static final HashMap<Long, LinkedList<User>> threadToListeningUsers = new HashMap<>(); // ключ: threadId / topicId, значение: пользователИ

    public interface RequestHandler {public void handle(Connection connection, JSONObject dataPacket);}

    static void connect(Socket connectionSocket){
        new Thread() {
            @Override
            public void run() {


                NotificationConnection connection = new NotificationConnection(connectionSocket);

                JSONObject dataPacket = null;
                try

                {
                    dataPacket = (JSONObject) connection.getRequest();
                } catch(IOException |
                        ClassNotFoundException e)

                {
                    System.out.println("Connection process dropped");
                }

                JSONObject reply = new JSONObject();
                String receivedToken = (String) dataPacket.get("token");
                if(receivedToken !=null)

                {
                    AuthTokenDAO authTokenDAO = new AuthTokenDAO();
                    AuthToken authToken = authTokenDAO.getByToken(receivedToken);
                    if (authToken == null) {
                        reply.put("status", "failure");
                        reply.put("desc", "can't get the token: incorrect format or it's empty");
                    } else {
                        UsersDAO usersDAO = new UsersDAO();
                        User user = usersDAO.getById(authToken.getUserid());
                        updateSubscriptions(user);
                        //synchronized (notificationConnections) {
                        notificationConnections.remove(user);
                        notificationConnections.put(user, connection);
                        // }
                        reply.put("status", "OK");
                        reply.put("desc", "notification starts");
                    }
                }else

                {
                    reply.put("status", "failure");
                    reply.put("desc", "incorrect token provided");
                }
                try

                {
                    connection.send(reply);
                } catch(
                        IOException e)

                {
                    throw new RuntimeException(e);
                }
            }
        }.start();
    }

    public static void updateSubscriptions(User user) {
        SubscriptionsDAO subscriptionsDAO = new SubscriptionsDAO();
        LinkedList<Subscription> userSubscriptions = subscriptionsDAO.getUserSubscriptions(user.getId());
        for(Subscription sub : userSubscriptions){
            if(threadToListeningUsers.containsKey(sub.getParentMessageId())){
                if(!threadToListeningUsers.get(sub.getParentMessageId()).contains(user))
                    threadToListeningUsers.get(sub.getParentMessageId()).add(user);
                System.out.println("USER "+user+" NOW LISTENING "+sub.getParentMessageId());
            }else {
                LinkedList<User> listeningUsers = new LinkedList<>();
                listeningUsers.add(user);
               // synchronized (threadToListeningUsers) {
                threadToListeningUsers.put(sub.getParentMessageId(), listeningUsers);
                //}
            }
        }
    }

    static synchronized void notifyInThread(Long threadId, JSONObject notification){
        Long isNotification = 1L;
        notification.put("isNotification", false);
        System.out.println("NOTIFICATION START");
        MessageDAO messageDAO = new MessageDAO();
        String threadPath = messageDAO.getByMessageId(threadId).getExtendedPath();
        //Pattern splitterPattern = Pattern.compile(".");
        String[] path = threadPath.split("\\.");
        System.out.println("PATH:" + threadPath);
        System.out.println("PATH:" + Arrays.toString(path));
        Stack<User> listenersToRemove = new Stack<>();
        HashSet<Long> ignoringUsers = new HashSet<>();
        boolean hasNotificationSet = false;
        List<String> list = Arrays.asList(path);
        Collections.reverse(list);
        HashSet<User> alreadySent = new HashSet<>();
        list.forEach(string -> {
            Long parentThreadId = Long.parseLong(string);
            System.out.println(parentThreadId);
            if(threadToListeningUsers.containsKey(parentThreadId)){
                LinkedList<User> listeningUsers = threadToListeningUsers.get(parentThreadId);
                for(User user : listeningUsers){
                    NotificationConnection connection = notificationConnections.get(user);
                    try {
                        if(!parentThreadId.equals(threadId))
                            notification.put("isNotification",true);
                        if(!alreadySent.contains(user)) {
                            connection.send(notification);
                            alreadySent.add(user);
                            System.out.println("NOTIFICATION SENT TO USER " + user.getUserName() + "ON thread " + parentThreadId);
                            ignoringUsers.add(user.getId());
                        }
                    } catch (IOException e) {
                        listenersToRemove.add(user);
                    }
                }
                for(User user : listenersToRemove){
                    listeningUsers.remove(user);
                    if(listeningUsers.isEmpty())
                        threadToListeningUsers.remove(parentThreadId);
                    notificationConnections.remove(user);
                }
                System.out.println("CURRENT NOTIFICATION LISTENING USERS:"+notificationConnections);
            }
            SubscriptionsDAO subscriptionsDAO = new SubscriptionsDAO();
            LinkedList<Subscription> subscriptions = subscriptionsDAO.getSubscriptionsToThread(parentThreadId);
            for(Subscription sub : subscriptions){
                if(!ignoringUsers.contains(sub.getUserId()) ){
                    LinkedList<Subscription> userSubscriptions = subscriptionsDAO.getUserSubscriptions(sub.getUserId());
                    Subscription subToCheck = new Subscription();
                    subToCheck.setUserId(sub.getUserId());
                    subToCheck.setParentMessageId(threadId);
                    if(!userSubscriptions.contains(subToCheck)){
                        Subscription userNotification = new Subscription();
                        userNotification.setUserId(sub.getUserId());
                        userNotification.setParentMessageId(threadId);
                        userNotification.setLastReadTime(0L);
                        userNotification.setNotification();
                        subscriptionsDAO.create(userNotification);
                    }
                }
            }
        });
    }

    static void notifyUser(User user, JSONObject notification){
        System.out.println("USER "+ user.getUserName() + " Notified about "+ notification.get("threadId"));
        try {
            NotificationConnection notificationConnection = notificationConnections.get(user);
            if(notificationConnection == null) return;
            notificationConnection.send(notification);
        } catch (IOException e) {
            //synchronized (notificationConnections){
                notificationConnections.remove(user);
            //}
        }
    }

}
