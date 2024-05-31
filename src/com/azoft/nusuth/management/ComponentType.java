package com.azoft.nusuth.management;

/*****************************************************************************
 *                                                                           *
 *                      (c) 2000-2001 Thruport Technologies                  *
 *                                                                           *
 *****************************************************************************/
public class ComponentType implements java.io.Serializable {
    public final static int CONTAINER = 0;
    public final static int DISTRIBUTOR = 1;
    public final static int DEPLOYER = 2;
    public final static int APPLICATION = 3;
    public final static String SCONTAINER = "container";
    public final static String SDISTRIBUTOR = "distributor";
    public final static String SDEPLOYER = "deployer";
    public final static String SAPPLICATION = "web-app";
    private int type = -1;


    public ComponentType(int componentType) throws ManagementException {
        type = componentType;
        if (componentType < 0 || componentType > 3)
            throw new ManagementException("Unknown component type \"" + componentType + '"');
    }


    public ComponentType(String componentType) throws ManagementException {
        if (componentType.equals(SCONTAINER))
            type = CONTAINER;
        else if (componentType.equals(SDISTRIBUTOR))
            type = DISTRIBUTOR;
        else if (componentType.equals(SDEPLOYER))
            type = DEPLOYER;
        else if (componentType.equals(SAPPLICATION))
            type = APPLICATION;
        else
            throw new ManagementException("Unknown component type \"" + componentType + '"');
    }


    public int getType() {
        return type;
    }


    private void setType(int newType) {
        type = newType;
    }


    public String toString() {
        switch (type) {
            case DISTRIBUTOR:
                return SDISTRIBUTOR;
            case CONTAINER:
                return SCONTAINER;
            case DEPLOYER:
                return SDEPLOYER;
            case APPLICATION:
                return SAPPLICATION;
            default :
                return "<unknown>";
        }
    }
}