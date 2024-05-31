package com.azoft.nusuth.management;

import java.util.*;
import java.util.Vector;

public class DeploingException extends ManagementException {
    public Vector notFindedContainers = new Vector();
    public Vector failedContainers = new Vector();


    public DeploingException(String s, Collection notFindedContainers, Collection failedContainers) {
        super(s);

        if (notFindedContainers != null && notFindedContainers.size() > 0) {
            this.notFindedContainers = new Vector(notFindedContainers.size());
            this.notFindedContainers.addAll(notFindedContainers);
        } else
            this.notFindedContainers = new Vector();

        if (failedContainers != null && failedContainers.size() > 0) {
            this.failedContainers = new Vector(failedContainers.size());
            this.failedContainers.addAll(failedContainers);
        } else
            this.failedContainers = new Vector();
    }
}