package com.azoft.nusuth.management;

/*****************************************************************************
 *                                                                           *
 *                      (c) 2000-2001 Thruport Technologies                  *
 *                                                                           *
 *****************************************************************************/

import java.util.*;
import java.io.Serializable;

import com.azoft.nusuth.deployment.*;

public class VirtualHostInfo implements java.io.Serializable {
    private String name;
    private String docBase;
    private HashMap applications;


    public VirtualHostInfo(CompositeNusuthWebAppElement node) throws DeploymentException {
        name = ManagementUtil.getSimpleString(node, "id");
        try {
            docBase = ManagementUtil.getSimpleString(node, "doc-base");
        } catch (DeploymentException dex) {
            docBase = "";
        }
        applications = new HashMap();
        for (Enumeration apps = node.getCompositeChild("context"); apps.hasMoreElements();) {
            CompositeNusuthWebAppElement appNode = (CompositeNusuthWebAppElement) apps.nextElement();
            String url;
            try {
                url = ManagementUtil.getSimpleString(appNode, "url");
            } catch (DeploymentException dex) {
                url = ManagementUtil.getSimpleString(appNode, "path");
            }
            if (!url.startsWith("/"))
                url = '/' + url;
            applications.put(url, new ApplicationInfoImpl(appNode));
        }
    }


    public VirtualHostInfo(String hostName, String docBase, HashMap applications) {
        name = hostName;
        this.applications = applications;
        this.docBase = docBase;
    }


    public HashMap getApplications() {
        return applications;
    }


    public String getName() {
        return name;
    }


    public String getDocBase() {
        return docBase;
    }


    public void merge(VirtualHostInfo host) {
        if (host.getName().equals(name)) {
            HashMap newApplications = host.getApplications();
            for (Iterator i = newApplications.keySet().iterator(); i.hasNext();) {
                String url = (String) i.next();
                ApplicationInfoImpl newApp = (ApplicationInfoImpl) newApplications.get(url);
                ApplicationInfoImpl oldApp = (ApplicationInfoImpl) applications.get(url);
                if (oldApp != null) {
                    oldApp.getContainers().addAll(newApp.getContainers());
                    oldApp.getProtocols().addAll(newApp.getProtocols());
                    String location = newApp.getLocation();
                    if (location != null && location.length() > 0)
                        oldApp.setLocation(location);
                } else
                    applications.put(url, newApp);
            }
        }
    }


    protected void setApplications(HashMap applications) {
        this.applications = applications;
    }


    protected void setName(String name) {
        this.name = name;
    }


    protected void setDocBase(String docBase) {
        this.docBase = docBase;
    }


    public String toString() {
        return '(' + name + applications + ')';
    }


    /*  public String toXml() {
      String result = "<host>\n" + "  <id>" + name + "</id>\n";
      for (Iterator i = applications.keySet().iterator(); i.hasNext();) {
        result += "  <context>\n";
        String url = (String) i.next();
        ApplicationInfoImpl app = (ApplicationInfoImpl) applications.get(url);
        result += "    <url>" + url + "</url>\n";
        String protocolString = app.getProtocols().toString();
        result += "    <protocol>" + protocolString.substring(1, protocolString.length() - 1) + "</protocol>";
        result += "    <is-running>" + app.isRunning() + "</is-running>\n";
        for (Iterator j = app.getContainers().iterator(); j.hasNext();) {
          result += "    <container>" + ((String) j.next()).trim() + "</container>\n";
        }
        result += "    <location>" + app.getLocation() + "</location>\n";
        result += "  </context>\n";
      }
      result += "</host>";
      return result;
     }*/




    public void addCompositeChild(CompositeNusuthWebAppElement node) throws DeploymentException {
        CompositeNusuthWebAppElement hostNode = node.addCompositeChild("host");
        hostNode.setSimpleChild("id").setContent(name);
//    if (docBase.length() > 0)
        hostNode.setSimpleChild("doc-base").setContent(docBase);

        for (Iterator i = applications.keySet().iterator(); i.hasNext();) {
            CompositeNusuthWebAppElement contextNode = hostNode.addCompositeChild("context");
            String url = (String) i.next();
            ApplicationInfoImpl app = (ApplicationInfoImpl) applications.get(url);

            contextNode.setSimpleChild("url").setContent(url);
            contextNode.setSimpleChild("is-running").setContent("" + app.isRunning());
            contextNode.setSimpleChild("location").setContent(app.getLocation());

            String protocolString = app.getProtocols().toString();
            contextNode.setSimpleChild("protocol").setContent(protocolString.substring(1, protocolString.length() - 1));

            for (Iterator j = app.getContainers().iterator(); j.hasNext();) {
                contextNode.addSimpleChild("container").setContent(((String) j.next()).trim());
            }
        }
    }


    public ApplicationInfoImpl getApplication(String appName) {
        return (ApplicationInfoImpl) applications.get(appName);
    }
}
