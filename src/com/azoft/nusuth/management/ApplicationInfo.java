package com.azoft.nusuth.management;

import java.util.HashSet;

public interface ApplicationInfo {
    int HTTP_PROTOCOL = 0;
    int HTTPS_PROTOCOL = 1;


    HashSet getContainers();


    void setContainers(HashSet containers);
}