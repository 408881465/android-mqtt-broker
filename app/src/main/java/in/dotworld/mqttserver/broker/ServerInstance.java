package in.dotworld.mqttserver.broker;

import io.moquette.server.Server;

public class ServerInstance {
    private static final Object INSTANCE_LOCK = new Object();
    private static Server serverInstance = null;

    public static Server getServerInstance() {
        try {
            if (serverInstance == null) {
                synchronized (INSTANCE_LOCK) {
                    if (serverInstance == null) {
                        serverInstance = new Server();
                        Server server = serverInstance;
                        return server;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return serverInstance;
    }
}
