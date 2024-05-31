/*
 * @(#)TextAreaPanelFactory.java 1.0 09/15/2001
 */

package com.azoft.nusuth.gui;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.border.EtchedBorder;

import com.azoft.nusuth.deployment.*;

/**
 * Class TextAreaPanelFactory is the unbounded panel factory
 * for the elements with only one child(!). For ex. web-app.listeners*
 *
 * @version 1.0 09/15/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class TextAreaPanelFactory extends UnboundedCompositeElementPanelFactory {

    /**
     * The main text area.
     */
    private JTextArea textArea;

    /**
     * In unbounded factory there are many composite elements with childName name.
     * Each of these element must have only one child.
     * Its name - the single children name.
     */
    private String singleChildName;


    /**
     * Constructs a new text area panel factory with the specified type,
     * children name & parent compoiste element.
     *
     * @param   type      the specified type.
     * @param   childName the specified child name.
     * @param   parent    the specified parent composite element.
     */
    public TextAreaPanelFactory(String type, String childName,
                                CompositeNusuthWebAppElement parent) {
        super(type, childName, parent);
        findSingleChildName();
    }


    /**
     * Finds the single child name.
     */
    private void findSingleChildName() {
        CompositeNusuthWebAppElement el = BasicPanel.getCompositeElement(getType());
        if (el != null) {
            Enumeration chNames = el.getSimpleChildrenNames();
            if (chNames != null && chNames.hasMoreElements()) {
                this.singleChildName = (String) chNames.nextElement();
            }
        }
    }

    /**
     * Sets the component names array for a tab naming.
     */
    protected void setTabNameComponents() {
    }

    /**
     * Gets the label string.
     *
     * @return  the label string.
     */
    private String getLabelString() {
        return DefaultEditorPanel.getDisplayName(getType()) + ":";
    }

    /**
     * Creates the panel with one component - text area.
     */
    public JComponent createPanel() {
        if (panel == null) {
            panel = new JPanel(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.gridheight = 1;
            c.weightx = 0.0;
            c.weighty = 0.0;
            c.insets = new Insets(0, 0, 5, 0);
            c.anchor = GridBagConstraints.WEST;
            panel.add(new JLabel(getLabelString()), c);
            c.weightx = 1.0;
            c.fill = GridBagConstraints.HORIZONTAL;
            panel.add(new JScrollPane(getTextArea()), c);
        }
        return panel;
    }

    /**
     * Gets the text area.
     *
     * @return  the text area
     */
    private JTextArea getTextArea() {
        if (textArea == null) {
            textArea = new JTextArea() {
                public Dimension getPreferredSize() {
                    Dimension superPref = super.getPreferredSize();
                    superPref.width = (superPref.width > 100) ? 100 : superPref.width;
                    return superPref;
                }
            };
            textArea.setRows(2);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(false);
            textArea.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) {
                    DefaultEditorPanel.needSave = true;
                }

                public void removeUpdate(DocumentEvent e) {
                    DefaultEditorPanel.needSave = true;
                }

                public void changedUpdate(DocumentEvent e) {
                    DefaultEditorPanel.needSave = true;
                }
            });
        }
        return textArea;
    }

    /**
     * Clears text area.
     */
    public void clearTextArea() {
        textArea.setText("");
    }

    /**
     * Gets the single chilren element content by the specified composite element.
     *
     * @param   element   the specified composite element.
     * @return  the single chilren element content
     * @see #updateElementBySingleChildContent(CompositeNusuthWebAppElement, String)
     */
    private String getSingleChildContent(CompositeNusuthWebAppElement element) {
        if (singleChildName != null) {
            try {
                Enumeration en = element.getSimpleChild(singleChildName);
                if (en != null && en.hasMoreElements()) {
                    return ((SimpleNusuthWebAppElement) en.nextElement()).getContent();
                }
            } catch (DeploymentException e) {
            }
        }
        return null;
    }

    /**
     * Updates the specified composite element by the specified single child content.
     *
     * @param   element   the specified composite element.
     * @param   content   the specified single child content.
     * @see #getSingleChildContent(CompositeNusuthWebAppElement)
     */
    private void
            updateElementBySingleChildContent(CompositeNusuthWebAppElement element,
                                              String content) {
        if (singleChildName != null && element != null && content != null) {
            try {
                Enumeration en = element.getSimpleChild(singleChildName);
                SimpleNusuthWebAppElement el = (en != null && en.hasMoreElements())
                        ? (SimpleNusuthWebAppElement) en.nextElement()
                        : element.setSimpleChild(singleChildName);
                el.setContent(content);
            } catch (DeploymentException e) {
            }
        }
    }

    /**
     * Adds the simple element content to this text area.
     *
     * @param   element   the specified composite element.
     */
    private void addCompositeElementValue(CompositeNusuthWebAppElement element) {
        String content = getSingleChildContent(element);
        if (content != null) {
            textArea.append((textArea.getText().equals("")) ? content : ";" + content);
        }
    }

    /**
     * Updates the controls by the specified parent composite element.
     * Adds the single child content from all composite children to text area.
     *
     * @param   parent    the specified parent composite element.
     * @see #updateElement(CompositeNusuthWebAppElement)
     */
    public void updateControls(CompositeNusuthWebAppElement parent) {
        clearTextArea();
        if (parent != null) {
            Enumeration e = null;
            try {
                e = parent.getCompositeChild(childName);
            } catch (DeploymentException de) {
                System.out.println(de);
            }
            while (e != null && e.hasMoreElements()) {
                CompositeNusuthWebAppElement webEl =
                        (CompositeNusuthWebAppElement) e.nextElement();
                addCompositeElementValue(webEl);
            }
        }
    }

    /**
     * Updates the specified parent composite element by the text area.
     * Creates the composite elements by the single child content.
     *
     * @param   parent    the specified parent composite element.
     * @see #updateControls(CompositeNusuthWebAppElement)
     */
    public void updateElement(CompositeNusuthWebAppElement parent) {
        if (parent != null) {
            Enumeration e = null;
            try {
                e = parent.getCompositeChild(childName);
            } catch (DeploymentException de) {
                System.out.println(de);
            }
            StringTokenizer st = new StringTokenizer(textArea.getText(), ";");
            Vector toDel = new Vector();
            while (e != null && e.hasMoreElements()) {
                CompositeNusuthWebAppElement webEl =
                        (CompositeNusuthWebAppElement) e.nextElement();
                try {
                    String content = st.nextToken();
                    if (!content.equals("")) {
                        updateElementBySingleChildContent(webEl, content);
                    } else {
                        toDel.addElement(webEl);
                    }
                } catch (NoSuchElementException nsee) { // if st hasn't the next element
                    toDel.addElement(webEl);
                }
            }
            for (int i = 0; i < toDel.size(); i++) {
                try {
                    parent.removeCompositeChild(childName,
                            (CompositeNusuthWebAppElement) toDel.elementAt(i));
                } catch (DeploymentException de) {
                    System.out.println(de);
                }
            }
            while (st.hasMoreElements()) {
                try {
                    CompositeNusuthWebAppElement webElement =
                            parent.addCompositeChild(childName);
                    updateElementBySingleChildContent(webElement, st.nextToken());
                } catch (DeploymentException dee) {
                    System.out.println(dee);
                }
            }
        }
    }

    /**
     * Return 0.
     * In this factory it's not important.
     *
     * @return  0
     */
/*
  public int getChildrensSize() {
    return 0;
  }
*/

    /**
     * If child is required in the parent element
     * this table have to containt one row at least.
     *
     * @return  <code>true</code> if all reqired elements are not empty;
     * <code>false</code> otherwise.
     */
    protected boolean requiredNotEmpty() {
        if (wrongCount()) {
            emptyChildFactory = null;
            emptyChild = childName;
            return false;
        }
        return true;
    }

    /**
     * Checks the text area content,
     * if child is required in the parent element.
     *
     * @return  <code>true</code> if child is required & text area content is empty;
     * <code>false</code> otherwise.
     */
    protected boolean wrongCount() {
        try {
            if (parent.isChildRequired(childName)
                    && getTextArea().getText().equals("")) {
                return true;
            }
        } catch (DeploymentException de) {
            System.out.println(de);
        }
        return false;
    }

    /**
     * Overrides the super method.
     * The textArea requests the focus.
     */
    protected void gotoEmpty() {
        getTextArea().requestFocus();
    }
}