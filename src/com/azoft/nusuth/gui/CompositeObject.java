package com.azoft.nusuth.gui;

public class CompositeObject {
    private boolean selected = true;
    private Object value;

    public CompositeObject(boolean selected, Object value) {
        this.selected = selected;
        this.value = value;
    }

    public boolean getSelected() {
        return this.selected;
    }

    public Object getValue() {
        return this.value;
    }

    public void setSelected(boolean b) {
        this.selected = b;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public boolean equals(Object otherObject) {
        if (otherObject != null && otherObject instanceof CompositeObject) {
            CompositeObject co = (CompositeObject) otherObject;
            if (co.getValue() != null) {
                return (co.getSelected() == getSelected()
                        && co.getValue().equals(getValue()));
            }
        }
        return false;
    }
}