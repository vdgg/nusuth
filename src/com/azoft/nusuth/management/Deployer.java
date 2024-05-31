package com.azoft.nusuth.management;

import java.io.InputStream;
import java.util.Vector;

public interface Deployer extends ComponentManager {


    void addApplication(Vector hosts) throws ManagementException;


    InputStream getWebInf(String docBase, String location) throws ManagementException;


    void patchApplication(Vector hosts, boolean overwrite) throws ManagementException;


    void replaceContent(Vector hosts) throws ManagementException;
}
