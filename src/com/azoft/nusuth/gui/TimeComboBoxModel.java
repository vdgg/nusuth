package com.azoft.nusuth.gui;

import javax.swing.*;
import java.util.*;

public class TimeComboBoxModel extends DefaultComboBoxModel {

    protected static Hashtable timeItems;

    static {
        timeItems = new Hashtable();
        timeItems.put(new Integer(1), "1 second");
        timeItems.put(new Integer(2), "2 seconds");
        timeItems.put(new Integer(3), "3 seconds");
        timeItems.put(new Integer(5), "5 seconds");
        timeItems.put(new Integer(10), "10 seconds");
        timeItems.put(new Integer(15), "15 seconds");
        timeItems.put(new Integer(30), "30 seconds");
        timeItems.put(new Integer(60), "1 minute");
        timeItems.put(new Integer(300), "5 minutes");
        timeItems.put(new Integer(600), "10 minutes");
        timeItems.put(new Integer(900), "15 minutes");
        timeItems.put(new Integer(1800), "30 minutes");
        timeItems.put(new Integer(3600), "1 hour");
    }

    public TimeComboBoxModel() {
        super();
    }

    public int getSeconds() {
        Object item = super.getSelectedItem();
        Enumeration e = timeItems.keys();
        while (e.hasMoreElements()) {
            Object key = e.nextElement();
            if (timeItems.get(key).equals(item)) {
                return ((Integer) key).intValue();
            }
        }
        return 0;
    }

    public void setSelectedSeconds(int sec) {
        if (timeItems.get(new Integer(sec)) != null)
            super.setSelectedItem(timeItems.get(new Integer(sec)));
    }

    public void setSelectedSeconds(String ssec) {
        try {
            int sec = Integer.parseInt(ssec);
            setSelectedSeconds(sec);
        } catch (Exception e) {
        }
    }
}


