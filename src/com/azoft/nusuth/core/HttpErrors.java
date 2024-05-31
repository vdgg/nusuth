/*
 * Created by IntelliJ IDEA.
 * To change template for new class use
 * "Source Code" options (Tools | IDE Options), Templates tab.
 */
package com.azoft.nusuth.core;

import java.util.ResourceBundle;
import java.util.Enumeration;

public class HttpErrors {
    private String[][] errorDescriptions = null;
    private final String NO_DESCRIPTION = "No Description";
    private org.apache.log4j.Category cat = org.apache.log4j.Category.getInstance("core");

    /**Constructor.
     */
    HttpErrors() {
        ResourceBundle bundle = ResourceBundle.getBundle("com.azoft.nusuth.core.HttpErrors");
        Enumeration enum = bundle.getKeys();
        int[] indexes = new int[9];
        int code;
        int index;
        int sin;
        int max = 0;
        while (enum.hasMoreElements()) {
            try {
                code = Integer.parseInt((String) enum.nextElement());
                index = code / 100 - 1;
                max = index > max ? index : max;
                sin = code % 100 + 1;
                if (sin > indexes[index]) {
                    indexes[index] = sin;
                }
            } catch (NumberFormatException nfe) {
                //Logger.log("Error in http errors file", 0);
                cat.warn("Error in http errors file");
                return;
            }
        }
        errorDescriptions = new String[max + 1][1];
        index = 0;
        String desc;
        while (indexes[index] > 0) {
            errorDescriptions[index] = new String[indexes[index]];
            index++;
        }
        for (int i = 0; i < errorDescriptions.length; i++) {
            for (int j = 0; j < errorDescriptions[i].length; j++) {
                desc = bundle.getString((new Integer((i + 1) * 100 + j)).toString());
                errorDescriptions[i][j] = desc == null ? NO_DESCRIPTION : desc;
                //System.out.println(((i + 1) * 100 + j) + " - " + desc);
            }
        }
    }

    /**This method returnes the error mesage , which depends on the given error code.
     * @param errorCode error code.
     * @return the corresponding error message.
     */
    String getErrorDescription(int errorCode) {
        if (errorDescriptions == null) {
            return NO_DESCRIPTION;
        }
        int findex = errorCode / 100 - 1;
        int sindex = errorCode % 100;
        return (findex < 0 || sindex < 0 || findex >= errorDescriptions.length || findex >= errorDescriptions[findex].length) ?
                NO_DESCRIPTION : errorDescriptions[findex][sindex];
    }
}

