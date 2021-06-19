package cc.kinami.audiotool.util;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class BeepWebSocketClient extends WebSocketClient {
    BeepWebSocketHandler webSocketHandler;

    public BeepWebSocketClient(URI serverUri) {
        super(serverUri, new Draft_6455());
    }

    public void setWebSocketHandler(BeepWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        webSocketHandler.onOpen(handshakedata);
    }

    @Override
    public void onMessage(String message) {
        webSocketHandler.onMessage(message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        webSocketHandler.onClose(code, reason, remote);
    }

    @Override
    public void onError(Exception ex) {
        webSocketHandler.onError(ex);
    }
}
