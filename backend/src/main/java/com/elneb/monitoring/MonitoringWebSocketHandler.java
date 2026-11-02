package com.elneb.monitoring;

import com.elneb.auth.JwtService;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MonitoringWebSocketHandler extends TextWebSocketHandler {
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final JwtService jwt;
    public MonitoringWebSocketHandler(JwtService jwt){this.jwt=jwt;}
    @Override public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token=session.getUri().getQuery()==null?null:java.net.URLDecoder.decode(session.getUri().getQuery().replaceFirst("^token=",""),java.nio.charset.StandardCharsets.UTF_8);
        if(token==null){session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Authentication required"));return;}
        try{Claims claims=jwt.parse(token);session.getAttributes().put("userId",claims.getSubject());sessions.add(session);}
        catch(RuntimeException ex){session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Invalid token"));}
    }
    @Override public void afterConnectionClosed(WebSocketSession session,CloseStatus status){sessions.remove(session);}
    public void broadcast(String type,Object data){String json="{\"type\":\""+type+"\",\"data\":"+data+"}";sessions.removeIf(s->{try{if(s.isOpen())s.sendMessage(new TextMessage(json));return false;}catch(Exception ex){return true;}});}
    public String json(Object value){if(value==null)return "null";return "\""+value.toString().replace("\"","\\\"")+"\"";}
}
