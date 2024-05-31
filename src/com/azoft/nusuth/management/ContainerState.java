package com.azoft.nusuth.management;

/*****************************************************************************
 *                                                                           *
 *                      (c) 2000-2001 Thruport Technologies                  *
 *                                                                           *
 *****************************************************************************/
public class ContainerState extends ServerState {

    CountInfo[] sessionCount;
    CountInfo[] requestCount;


    public CountInfo[] getRequestCount() {
        return requestCount;
    }


    public int getRequestCount(String appUrl) {
        for (int i = 0; i < requestCount.length; i++) {
            if (requestCount[i].getSubject().equals(appUrl)) {
                return requestCount[i].getCount();
            }
        }
        return -1;
    }


    public CountInfo[] getSessionCount() {
        return sessionCount;
    }


    public int getSessionCount(String appUrl) {
        for (int i = 0; i < sessionCount.length; i++) {
            if (sessionCount[i].getSubject().equals(appUrl)) {
                return sessionCount[i].getCount();
            }
        }
        return -1;
    }


    public int getTotalRequestCount() {
        int result = 0;
        for (int i = 0; i < requestCount.length; i++) {
            result += requestCount[i].getCount();
        }
        return result;
    }


    public int getTotalSessionCount() {
        int result = 0;
        for (int i = 0; i < sessionCount.length; i++) {
            result += sessionCount[i].getCount();
        }
        return result;
    }

    protected void setSessionCount(CountInfo[] sessionCount) {
        this.sessionCount = sessionCount;
    }

    protected void setRequestCount(CountInfo[] requestCount) {
        this.requestCount = requestCount;
    }
}
