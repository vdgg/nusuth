package com.azoft.nusuth.gui;

import com.azoft.nusuth.deployment.*;

import java.io.File;
import java.util.Properties;

import org.xml.sax.EntityResolver;

public class CompositeElementFactory {
    private File fJBirdHome;
    private static Properties props;

    static {
        props = new Properties();
        try {
            props.load(ClassLoader.getSystemClassLoader().
                    getResourceAsStream("com/azoft/nusuth/gui/XMLNames.properties"));
        } catch (Throwable ex) {
            System.err.println(ex);
            System.err.println("Cannot load properties - using names instead");
        }
    }

    public CompositeElementFactory() {
        NusuthAppConfigFactory.addEntityResolver(BasicPanel.SCONTAINER,
                (EntityResolver) new ContainerEntityResolver());
        NusuthAppConfigFactory.addEntityResolver(BasicPanel.SWEB_APP,
                (EntityResolver) new WebEntityResolver());
        NusuthAppConfigFactory.addEntityResolver(BasicPanel.SAPP_DEP,
                (EntityResolver) new ApplicationDeploymentEntityResolver());
        NusuthAppConfigFactory.addEntityResolver(BasicPanel.SDISTRIBUTOR,
                (EntityResolver) new DistributorEntityResolver());
//    NusuthAppConfigFactory.addEntityResolver(BasicPanel.SDEPLOYER,
//            (EntityResolver) new DeployerEntityResolver());
        NusuthAppConfigFactory.addEntityResolver(BasicPanel.SSECURITY,
                (EntityResolver) new SecurityConfigEntityResolver());
        NusuthAppConfigFactory.addEntityResolver(BasicPanel.SWEBTAGLIB,
                (EntityResolver) new JspEntityResolver());
        NusuthAppConfigFactory.addEntityResolver(BasicPanel.SWEBUSERS,
                (EntityResolver) new WebAppUsersEntityResolver());
        this.fJBirdHome = new File("../admin");
    }

    public CompositeNusuthWebAppElement getWebElement(String type) {
        try {
            String name = (props.getProperty(type + ".name") == null)
                    ? type : props.getProperty(type + ".name");
            return NusuthAppConfigFactory.createConfig(
                    type, fJBirdHome + File.separator + name + "empty.xml");
        } catch (com.azoft.nusuth.deployment.ParserException e) {
            System.out.println(e);
        }
        return null;
    }
}