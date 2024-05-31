package com.azoft.nusuth.management;

import java.util.HashSet;

public interface DistributorApplicationInfo extends ApplicationInfo {


    HashSet getProtocols();


    void setProtocols(HashSet protocols);
}