package com.azoft.nusuth.session;

import java.net.SocketException;

public interface SessionWorkOverListener {

    public void onSessionWorkOver(String sessionId) throws SocketException;

    public boolean changeId(String sessionId, String containerId);

    public void changeRemoteId(String sessionId);

}