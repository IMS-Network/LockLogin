package eu.locklogin.api.common.web.services;

import eu.locklogin.api.common.web.services.socket.SocketClient;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.engineio.client.transports.Polling;
import io.socket.engineio.client.transports.WebSocket;
import ml.karmaconfigs.api.common.utils.url.URLUtils;
import okhttp3.OkHttpClient;

import java.net.URI;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static io.socket.client.IO.Options;

public class LockLoginSocket implements SocketClient {

    private final URI uri;
    private final static int port = 8443;

    private static Socket socket = null;

    public LockLoginSocket() {
        String[] tries = new String[]{
                "https://backup.karmadev.es/",
                "https://backup.karmaconfigs.ml/",
                "https://backup.karmarepo.ml/",
                "https://karmadev.es/",
                "https://karmaconfigs.ml/",
                "https://karmarepo.ml/"
        };

        URL working = URLUtils.getOrBackup(tries);
        URI tmpUri = URI.create("https://karmadev.es:" + port + "/");
        if (URLUtils.getOrBackup("https://karmadev.es/") == null) {
            if (working != null) {
                try {
                    tmpUri = URI.create("https://" + working.toURI().getHost() + ":" + port + "/");
                } catch (Throwable ignored) {
                }
            }
        }

        uri = tmpUri;
    }

    /**
     * Get the statistic server
     *
     * @return the statistic server
     */
    @Override
    public URI server() {
        return uri;
    }

    /**
     * Get the socket client
     *
     * @return the socket client
     */
    @Override
    public Socket client() {
        if (socket == null || !socket.connected()) {
            Options options = new Options();
            options.secure = true;
            options.multiplex = false;
            options.forceNew = true;
            options.transports = new String[]{Polling.NAME, WebSocket.NAME};
            options.upgrade = true;
            options.rememberUpgrade = true;
            options.reconnection = true;
            options.reconnectionAttempts = 5;
            options.reconnectionDelay = 1;
            options.reconnectionDelayMax = 5;
            options.randomizationFactor = 0.5;
            options.auth = new ConcurrentHashMap<>();
            options.auth.put("communication_preference", "");
            //TODO: Expect this to be generated from LockLogin download server
            /*for (Method m : SocketHelper.class.getDeclaredMethods()) {
                try {
                    Field f = m.getClass().getDeclaredFields()[0];
                    f.setAccessible(true);
                    options.auth.put((String) f.get(m), (String) m.invoke(null));
                    f.set(m, StringUtils.generateString().create());
                    f.setAccessible(false);
                    m.setAccessible(false);

                    //Fix method
                    Field mods = m.getClass().getDeclaredField("modifiers");
                    mods.setAccessible(true);
                    mods.set(m, Modifier.NATIVE);
                    mods.setAccessible(false);
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            }*/

            OkHttpClient client = new OkHttpClient.Builder()
                    .readTimeout(1, TimeUnit.MINUTES)
                    .build();

            options.callFactory = client;
            options.webSocketFactory = client;

            socket = IO.socket(uri, options);
        }

        return socket;
    }
}
