package com.azoft.nusuth.jidep;

/**
 * This class is used for simplification for notification type.
 * @author skilz
 * @since Nusuth1.0
 * @version 1.0
 */
public class NotificationType {

    public static final int CREATED = 1;
    public static final int DELETED = 2;
    public static final int REBINDED = 3;
    public static final int ATTRCHANGED = 4;

    public static final String[] names = {
        "<wrong notification type (0)>", // 0
        "created", // 1
        "deleted", // 2
        "rebinded", // 3
        "attributes changed"              // 4
    };
}
