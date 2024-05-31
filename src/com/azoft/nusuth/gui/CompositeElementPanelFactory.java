/*
 * @(#)CompositeElementPanelFactory.java 1.0 02/25/2001
 */

package com.azoft.nusuth.gui;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.IOException;

import com.azoft.nusuth.deployment.*;
import com.azoft.nusuth.management.ComponentType;

/**
 * Class CompositeElementPanelFactory is the class for one composite element.
 * It stores all renderers for simple elements &
 * all composite factories for composite elements.
 * It controlls all updating - updateControls & updateElement.
 * Stores all composite element states - required, unbounded & others.
 *
 * @version 1.0 02/25/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class CompositeElementPanelFactory {
    private static Properties disabledProps;
    private static Hashtable enabled = new Hashtable();

    /**
     * The factory tag.
     */
    protected String tag;

    /**
     * The factory type.
     */
    protected String type;

    /**
     * The simple panel witl all simple elements.
     */
    protected JPanel simplePanel;

    /**
     * hash: simple child name -> child element renderer
     */
    protected Hashtable renderers = new Hashtable();

    /**
     * hash: composite child name -> composite factory
     */
    protected Hashtable compositeFactories = null;

    /**
     * The composite element for this factory.
     */
    protected CompositeNusuthWebAppElement webElement;

    /**
     * vector of disabled elements.
     */
    protected Vector disabled = new Vector();

    /**
     * vector of factories in basics tab.
     */
//  protected Vector factoriesInBasics = null;

    /**
     * vector of tabs in this factory.
     */
    protected Vector tabs = null;

    /**
     * the empty child name
     */
    protected String emptyChild = "";

    /**
     * the empty child factory panel
     * used when goes to empty element
     */
    protected CompositeElementPanelFactory emptyChildFactory = null;


//  private String type;
    private boolean individualTab = true;
    private boolean requared = true;
    private JCheckBox activeCheck;
    private JComponent mainComponent;
    private ButtonGroup buttonGroup;
    private ActionListener radioButtonListener;
    private Hashtable radioHashComps;
    private Hashtable radioHashNames;
    private TabNameChangedListener tabNameChangedListener;
    private PasswordElementRenderer passwordElementRenderer = null;
    private Hashtable nameValueComponent = new Hashtable();
    // childName -> critical value, enabled component


    static {
        disabledProps = new Properties();
        try {
            disabledProps.load(ClassLoader.getSystemResourceAsStream(
                    "com/azoft/nusuth/gui/disabledElements.properties"));
        } catch (IOException e) {
            System.out.println("can not load disabled elements properties");
        }
        enabled.put("web-app.login-config.auth-method.FORM", "form-login-config");
    }


    /**
     * Constructs a new composite element panel factory with
     * the specified type.
     *
     * @param   type    the specified panel factory type.
     */
    public CompositeElementPanelFactory(String type) {
        this(type, BasicPanel.getCompositeElement(type));
    }

    /**
     * Constructs a new composite element panel factory with
     * the specified type & composite element.
     *
     * @param   type        the specified panel factory type.
     * @param   webElement  the specified webElement.
     */
    public CompositeElementPanelFactory(String type,
                                        CompositeNusuthWebAppElement webElement) {
        super();
        this.type = type;
        this.tag = (webElement == null) ? "unknown" : webElement.getTag();
        this.webElement = webElement;
//    if (path != null) setPath(path);
        initRenderers();
        getCompositePanelFactories();
    }


    /**
     * Creates the composite element for this panel factory.
     *
     * @return  the created composite element
     */
    protected CompositeNusuthWebAppElement createCompositeElement() {
        return createCompositeElement(this.type);
    }

    /**
     * Creates the composite element by the specified type.
     *
     * @param   type    the specified type.
     * @return  the created composite element
     */
    protected CompositeNusuthWebAppElement createCompositeElement(String type) {
        return BasicPanel.getCompositeElement(type);
    }

    public void setRequired(boolean requared) {
        this.requared = requared;
    }

    public boolean isRequired() {
        return this.requared;
    }

    public void setActive(boolean active) {
        getActiveCheckBox().setSelected(active);
        enableAll(active);
        // clear all renderers if not individual tab
        if (!active && !isIndividualTab()) {
            Enumeration e = renderers.elements();
            while (e.hasMoreElements()) {
                ElementRenderer renderer = (ElementRenderer) e.nextElement();
                renderer.setValue("");
            }
        }
    }

    public void disableActive() {
        setActive(true);
        getActiveCheckBox().setEnabled(false);
    }

    public boolean getActive() {
        if (isIndividualTab()) {
            return getActiveCheckBox().isSelected();
        }
        Enumeration e = renderers.elements();
        while (e.hasMoreElements()) {
            ElementRenderer renderer = (ElementRenderer) e.nextElement();
            if (renderer.getComponent().isEnabled() && !renderer.isContentEmpty()) {
                if (renderer instanceof CheckBooleanElementRenderer
                        && renderer.getValue().equals("false")) {
                    continue;
                }
                return true;
            }
        }
        if (compositeFactories != null) {
            Enumeration en = compositeFactories.keys();
            while (en.hasMoreElements()) {
                String childName = (String) en.nextElement();
                CompositeElementPanelFactory fac =
                        (CompositeElementPanelFactory) compositeFactories.get(childName);
                if (fac.getActive()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean fromEnabled(String name) {
        Enumeration keys = enabled.keys();
        while (keys.hasMoreElements()) {
            String nextKey = (String) keys.nextElement();
            if (nextKey.startsWith(type + "." + name)) {
                String[] value = new String[2];
                // critical value
                value[0] = nextKey.substring((type + "." + name).length() + 1);
                // enabled component name
                value[1] = (String) enabled.get(nextKey);
                nameValueComponent.put(name, value);
                return true;
            }
        }
        return false;
    }

    private String getEnabledComponentRendererName(String childName) {
        Enumeration keys = nameValueComponent.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            String[] value = (String[]) nameValueComponent.get(key);
            if (value[1].equals(childName)) {
                return key;
            }
        }
        return null;
    }

    private void setEnabledComponent(String rendererName) {
/*
    String itemValue = getValue(rendererName);
    String[] value = (String[]) nameValueComponent.get(rendererName);
    CompositeElementPanelFactory fac = getCompositePanelFactory(value[1]);
    boolean enabled = itemValue.equals(value[0]);
    String tabTitle = DefaultEditorPanel.getDisplayName(fac.getType());
    int curIndexOfEnComp = ((JTabbedPane) mainComponent).indexOfTab(tabTitle);
    ((JTabbedPane) mainComponent).setEnabledAt(curIndexOfEnComp, enabled);
    fac.setActive(enabled);
*/
    }

    /**
     * Adds the child component with the specified name to the
     * simple panel with the specified grid bag constraints.
     *
     * @param   childName the specified child name.
     * @param   c         the specified grid bag constraints.
     * @see #addChildComponents(JPanel,String,GridBagConstraints)
     */
    protected void addChildComponents(String childName, GridBagConstraints c) {
        addChildComponents(simplePanel, childName, c);
    }

    /**
     * Adds the child component with the specified name to the
     * specified panel with the specified grid bag constraints.
     *
     * @param   panel     the specified panel.
     * @param   childName the specified child name.
     * @param   c         the specified grid bag constraints.
     * @see #addChildComponents(String,GridBagConstraints)
     */
    protected void addChildComponents(JPanel panel, String childName, GridBagConstraints c) {
        boolean requared = false;
        boolean choiced = false;
        boolean unbounded = false;
        try {
            requared = webElement.isChildRequired(childName);
            choiced = webElement.getChildChoices(childName).hasMoreElements();
            unbounded = webElement.isChildUnbounded(childName);
        } catch (DeploymentException de) {
        }
        ElementRenderer renderer = (ElementRenderer) renderers.get(childName);
        if (choiced) {
            DefaultEditorPanel.createRow(renderer, panel, c, type, childName,
                    requared, unbounded, getButtonGroup(),
                    getRadioButtonActionListener(), getRadioHashtComps(),
                    getRadioHashNames());
            boolean has = false;
            try {
                Enumeration se = webElement.getSimpleChild(childName);
                has = (se != null && se.hasMoreElements());
            } catch (DeploymentException e) {
            }
            JRadioButton radio = getRadioButton(childName);
            if (radio != null) {
                radio.setSelected(has);
                enableRadioComponents(radio, has);
            }
        } else {
            DefaultEditorPanel.createRow(renderer, panel, c, type, childName,
                    requared, unbounded);
        }
    }

    public boolean hasSimplePanel() {
        if (webElement == null) return false;
        return webElement.getSimpleChildrenNames().hasMoreElements();
    }

    protected void initChildRenderer(final String childName) {
//    ElementRenderer renderer = WebElementPanel.getRenderer(tag, childName);
        ElementRenderer renderer = DefaultEditorPanel.getRenderer(type, childName);
        renderers.put(childName, renderer);
        if (renderer instanceof PasswordElementRenderer) {
            passwordElementRenderer = (PasswordElementRenderer) renderer;
        }
        if (fromEnabled(childName)) {
            renderer.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setEnabledComponent(childName);
                }
            });
        }
    }

    protected void initRenderers() {
        if (webElement == null) {
            webElement = createCompositeElement();
        }
        if (webElement != null) {
            Enumeration e = webElement.getSimpleChildrenNames();
            while (e.hasMoreElements()) {
                initChildRenderer((String) e.nextElement());
            }
        }
    }

    /**
     * Gets the simple panel - the panel with simple elements.
     * This panel is for BASICS tab.
     *
     * @return  the simple panel
     */
    public JPanel getSimplePanel() {
        if (simplePanel == null) {
            GridBagLayout gridbag = new GridBagLayout();
            simplePanel = new JPanel(gridbag);
            if (webElement != null) {
                Enumeration e = webElement.getSimpleChildrenNames();
                GridBagConstraints c = new GridBagConstraints();
                c.gridy = -1;
                c.insets = new Insets(24, 4, 4, 4);
                if (e.hasMoreElements()) {
                    addChildComponents((String) e.nextElement(), c);
                }
                c.insets = new Insets(4, 4, 4, 4);
                while (e.hasMoreElements()) {
                    addChildComponents((String) e.nextElement(), c);
                }
                c.gridx = 0;
                c.gridy++;
                c.gridwidth = 2;
                c.weighty = 0.0;
                if (!requared) {
                    simplePanel.add(getActiveCheckBox(), c);
                }
                c.gridy++;
                c.weighty = 0.05;
                simplePanel.add(new JPanel(), c);
                if (passwordElementRenderer != null) {
                    ((DefaultElementRenderer) renderers.get("name")).
                            setPasswordEnabledListener(passwordElementRenderer);
                }
            }
        }
        return simplePanel;
    }

    protected JCheckBox getActiveCheckBox() {
        if (activeCheck == null) {
            activeCheck = new JCheckBox("activate?");
            activeCheck.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    DefaultEditorPanel.needSave = true;
                    JCheckBox ch = (JCheckBox) e.getSource();
                    enableAll(ch.isSelected());
                }
            });
        }
        return activeCheck;
    }

    private void enableAll(boolean state) {
        if (mainComponent instanceof JTabbedPane) {
            JTabbedPane pane = (JTabbedPane) mainComponent;
            for (int i = 1; i < pane.getTabCount(); i++) {
                pane.setEnabledAt(i, state);
            }
        }
        if (state) {
            Enumeration keys = nameValueComponent.keys();
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                setEnabledComponent(key);
            }
        }
        if (simplePanel != null) {
            int csize = simplePanel.getComponentCount();
            for (int i = 0; i < csize; i++) {
                Component comp = simplePanel.getComponent(i);
                if (!(comp instanceof JCheckBox)) {
                    comp.setEnabled(state);
                }
            }
        }
    }

    protected ButtonGroup getButtonGroup() {
        if (buttonGroup == null) {
            buttonGroup = new ButtonGroup();
        }
        return buttonGroup;
    }

    protected ActionListener getRadioButtonActionListener() {
        if (radioButtonListener == null) {
            radioButtonListener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    DefaultEditorPanel.needSave = true;
                    JRadioButton radio = (JRadioButton) e.getSource();
                    Enumeration en = getButtonGroup().getElements();
                    while (en.hasMoreElements()) {
                        JRadioButton nextRadio = (JRadioButton) en.nextElement();
                        enableRadioComponents(nextRadio, nextRadio == radio);
                    }
                }
            };
        }
        return radioButtonListener;
    }

    protected void enableRadioComponents(JRadioButton radio, boolean b) {
        JComponent[] comps = (JComponent[]) getRadioHashtComps().get(radio);
        for (int i = 0; i < comps.length; i++) {
            comps[i].setEnabled(b);
        }
    }

    protected Hashtable getRadioHashtComps() {
        if (radioHashComps == null) {
            radioHashComps = new Hashtable();
        }
        return radioHashComps;
    }

    protected Hashtable getRadioHashNames() {
        if (radioHashNames == null) {
            radioHashNames = new Hashtable();
        }
        return radioHashNames;
    }

    public JRadioButton getRadioButton(String childName) {
        return (JRadioButton) getRadioHashNames().get(childName);
    }

    public String getValue(String componentName) {
        ElementRenderer renderer = (ElementRenderer) renderers.get(componentName);
        return (renderer == null) ? "" : renderer.getValue();
    }

    public Enumeration getValues(String componentName) {
        ElementRenderer renderer = (ElementRenderer) renderers.get(componentName);
        if (renderer == null) return null;
        if (renderer instanceof CheckBoxElementRenderer) {
            return ((CheckBoxElementRenderer) renderer).getValues();
        }
        if (renderer instanceof TableRenderer) {
            return ((TableRenderer) renderer).getValues();
        }
        return null;
    }

    public void setValue(String componentName, String value) {
        ElementRenderer renderer = (ElementRenderer) renderers.get(componentName);
        if (renderer != null) {
            renderer.setValue(value);
        }
    }

    public void setValues(String componentName, Enumeration en) {
        ElementRenderer renderer = (ElementRenderer) renderers.get(componentName);
        if (renderer != null) {
            if (renderer instanceof CheckBoxElementRenderer) {
                ((CheckBoxElementRenderer) renderer).setValues(en);
            }
            if (renderer instanceof TableRenderer) {
                ((TableRenderer) renderer).setValues(en);
            }
        }
    }

    public String getTag() {
        return tag;
    }

    public String getType() {
        return type;
    }

    public boolean hasCompositePanel() {
        return (webElement == null) ? false :
                webElement.getCompositeChildrenNames().hasMoreElements();
    }

    /**
     * Gets the enumeration of the child factories.
     * This enumeration contains the factories for composite elements.
     * Inits the factories hashtable if necessary.
     *
     * @return  the enumeration of the child factories.
     * @see #getCompositePanelFactory(String)
     */
    public Enumeration getCompositePanelFactories() {
        if (webElement == null) {
            return null;
        }
        if (compositeFactories == null) {
            compositeFactories = new Hashtable();
            Enumeration e = webElement.getCompositeChildrenNames();
            while (e.hasMoreElements()) {
                String childName = (String) e.nextElement();
                if (disabledProps.get(getType() + "." + childName) != null) {
                    disabled.addElement(childName);
                } else {
                    try {
                        CompositeElementPanelFactory factory = null;
                        if (webElement.isChildUnbounded(childName)) {
//                      || type.equals(BasicPanel.SSERVLET)
//                      || type.equals(BasicPanel.SFILTER)
                            factory = new TablePanelFactory(this.type + "." + childName,
                                    childName, webElement);
/*
              factory = (type.startsWith(BasicPanel.SWEB_APP)
                      || type.startsWith(BasicPanel.SDISTRIBUTOR)
                      || type.startsWith(BasicPanel.SWEBTAGLIB))
                      ? new TablePanelFactory(this.type + "." + childName,
                              childName, webElement)
                      : new UnboundedCompositeElementPanelFactory(
                              this.type + "." + childName,
                              childName, webElement);
*/
                        } else {
                            Enumeration en = webElement.getCompositeChild(childName);
                            CompositeNusuthWebAppElement el = null;
                            if (en != null && en.hasMoreElements()) {
                                el = (CompositeNusuthWebAppElement) en.nextElement();
                            }
                            factory = new CompositeElementPanelFactory(
                                    this.type + "." + el.getTag(), el);
                            factory.setRequired(webElement.isChildRequired(childName));
                        }
                        if (factory != null) {
//              factory.setIndividualTab(!factoriesInBasics.contains(childName));
                            compositeFactories.put(childName, factory);
                        }
                    } catch (DeploymentException de) {
                    }
                }
            }
        }
        return compositeFactories.elements();
    }

    public CompositeElementPanelFactory getCompositePanelFactory(String name) {
        if (compositeFactories == null) {
            getCompositePanelFactories(); // this method inits hashtable if it null
            if (compositeFactories == null) {
                return null;
            }
        }
        return (CompositeElementPanelFactory) compositeFactories.get(name);
    }

    public JComponent createPanel() {
        if (mainComponent == null) {
            if (!hasCompositePanel()) {
                mainComponent = new JScrollPane(getSimplePanel());
            } else {
                mainComponent = new JTabbedPane(JTabbedPane.TOP);
                ((JTabbedPane) mainComponent).addTab(
                        EditorPanel.BASICS, new JScrollPane(getSimplePanel()));
                Enumeration e = getCompositePanelFactories();
                while (e.hasMoreElements()) {
                    CompositeElementPanelFactory factory =
                            (CompositeElementPanelFactory) e.nextElement();
                    if (factory.isIndividualTab()) {
                        ((JTabbedPane) mainComponent).addTab(
                                DefaultEditorPanel.getDisplayName(factory.getType()),
                                factory.createPanel());
                    }
                }
            }
        }
        return mainComponent;
    }

    public void clear() {
    }

    /**
     * Updates all panel components by the specified web element.
     *
     * @param   webElement  the specified web element.
     * @see #updateElement(CompositeNusuthWebAppElement)
     */
    public void updateControls(CompositeNusuthWebAppElement webElement) {
        if (webElement != null) {
            Enumeration e = webElement.getSimpleChildrenNames();
            while (e.hasMoreElements()) {
                String childName = (String) e.nextElement();
                if (renderers.get(childName) != null) {
                    Enumeration se = null;
                    try {
                        se = webElement.getSimpleChild(childName);
                        if (webElement.isChildUnbounded(childName)) {
                            ElementRenderer renderer =
                                    (ElementRenderer) renderers.get(childName);
                            if (renderer instanceof CheckBoxElementRenderer
                                    || renderer instanceof TableRenderer) {
                                setValues(childName, se);
                            } else {
                                String res = "";
                                boolean fir = true;
                                while (se != null && se.hasMoreElements()) {
                                    if (!fir) res += ";";
                                    res += ((SimpleNusuthWebAppElement) se.nextElement()).getContent();
                                    fir = false;
                                }
                                setValue(childName, res);
                            }
                        } else {
                            boolean has = (se != null && se.hasMoreElements());
                            String text = (has)
                                    ? ((SimpleNusuthWebAppElement) se.nextElement()).getContent()
                                    : "";
                            setValue(childName, text);
                            JRadioButton radio = getRadioButton(childName);
                            if (radio != null && has) {
                                radio.doClick();
                            }
                        }
                    } catch (DeploymentException de) {
                        System.out.println(de);
                    }
                }
            }
            e = webElement.getCompositeChildrenNames();
            while (e.hasMoreElements()) {
                String childName = (String) e.nextElement();
                if (!disabled.contains(childName)) {
                    try {
                        CompositeElementPanelFactory factory =
                                getCompositePanelFactory(childName);
                        if (factory != null) {
                            if (webElement.isChildUnbounded(childName)) {
                                factory.updateControls(webElement);
                            } else {
                                Enumeration ce = webElement.getCompositeChild(childName);
                                if (ce != null && ce.hasMoreElements()) {
                                    CompositeNusuthWebAppElement webEl =
                                            (CompositeNusuthWebAppElement) ce.nextElement();
                                    factory.updateControls(webEl);
                                    factory.setActive(true);
                                } else {
                                    // have no such composite elements
                                    factory.setActive(false);
                                }
                            }
                        }
                    } catch (DeploymentException de) {
                        System.out.println(de);
                    }
                }
            }
        }
    }

    /**
     * Updates the specified web element by all panel components.
     *
     * @param   webElement  the specified web element.
     * @see #updateControls(CompositeNusuthWebAppElement)
     */
    public void updateElement(CompositeNusuthWebAppElement webElement) {
        if (webElement != null) {
            Enumeration e = webElement.getSimpleChildrenNames();
            while (e.hasMoreElements()) {
                String childName = (String) e.nextElement();
                SimpleNusuthWebAppElement simpleElement = null;
                try {
                    if (webElement.isChildUnbounded(childName)) {
                        Enumeration enum = webElement.getSimpleChild(childName);
                        Vector toDel = new Vector();
                        while (enum.hasMoreElements()) {
                            toDel.addElement(enum.nextElement());
                        }
                        Enumeration en = null;
                        ElementRenderer renderer =
                                (ElementRenderer) renderers.get(childName);
                        if (renderer instanceof CheckBoxElementRenderer
                                || renderer instanceof TableRenderer) {
                            en = getValues(childName);
                        } else {
                            String resValue = getValue(childName);
                            en = new StringTokenizer(resValue, ";");
                        }
                        while (en != null && en.hasMoreElements()) {
                            simpleElement = webElement.addSimpleChild(childName);
                            simpleElement.setContent(((String) en.nextElement()).trim());
                        }
                        for (int i = 0; i < toDel.size(); i++) {
                            webElement.removeSimpleChild(childName,
                                    (SimpleNusuthWebAppElement) toDel.elementAt(i));
                        }
                    } else {
                        JRadioButton radio = getRadioButton(childName);
                        if (radio == null || (radio != null && radio.isSelected())) {
                            if (getValue(childName) != null
                                    && getValue(childName).trim().length() > 0) {
                                simpleElement = webElement.setSimpleChild(childName);
                                simpleElement.setContent(getValue(childName));
                            } else if (!webElement.isChildRequired(childName)
                                    && webElement.getSimpleChild(childName).hasMoreElements()) {
                                webElement.removeSimpleChild(childName,
                                        (SimpleNusuthWebAppElement) webElement.
                                        getSimpleChild(childName).nextElement());
                            }
                        }
                    }
                } catch (DeploymentException de) {
                    System.out.println(de);
                }
            }
            e = webElement.getCompositeChildrenNames();
            while (e.hasMoreElements()) {
                String childName = (String) e.nextElement();
                if (!disabled.contains(childName)) {
                    try {
                        CompositeElementPanelFactory factory =
                                getCompositePanelFactory(childName);
                        if (factory != null) {
                            if (webElement.isChildUnbounded(childName)) {
                                factory.updateElement(webElement);  // factory - unbounded
                            } else {
                                Enumeration ce = webElement.getCompositeChild(childName);
                                boolean active = factory.getActive()
                                        || webElement.isChildRequired(childName);
                                if (ce != null && ce.hasMoreElements()) {
                                    CompositeNusuthWebAppElement webEl =
                                            (CompositeNusuthWebAppElement) ce.nextElement();
                                    if (active) {
                                        factory.updateElement(webEl);
                                    } else {
                                        webElement.removeCompositeChild(childName, webEl);
                                    }
                                } else {
                                    if (active) {
                                        CompositeNusuthWebAppElement webEl =
                                                webElement.setCompositeChild(childName);
                                        factory.updateElement(webEl);
                                    }
                                }
                            }
                        }
                    } catch (DeploymentException de) {
                        System.out.println(de);
                    }
                }
            }
        }
    }

    public void updateUI() {
        getSimplePanel().updateUI();
        Enumeration e = getCompositePanelFactories();
        while (e != null && e.hasMoreElements()) {
            CompositeElementPanelFactory factory =
                    (CompositeElementPanelFactory) e.nextElement();
            if (factory.isIndividualTab()) factory.updateUI();
        }
    }

    public void setTabNameChangers(String[] childs, TabNameChangedListener l) {
        this.tabNameChangedListener = l;
        for (int i = 0; i < childs.length; i++) {
            String childName = childs[i];
            ElementRenderer renderer = (ElementRenderer) renderers.get(childName);
            if (renderer != null && renderer instanceof FixValuesElementRenderer) {
                FixValuesElementRenderer fixvr = (FixValuesElementRenderer) renderer;
                fixvr.setTabNameChangedListener(new TabNameChangedListener() {
                    public void tabNameChanged(String newName) {
                        newName = (newName == null || newName.equals(""))
                                ? DefaultEditorPanel.getDisplayName(getType()) : newName;
                        CompositeElementPanelFactory.this.
                                tabNameChangedListener.tabNameChanged(newName);
                    }
                });
            }
        }
    }

    /**
     * Gets the display for this panel factory.
     *
     * @return  the display for this panel factory.
     */
    public String getDisplay() {
        String displayComp =
                DefaultEditorPanel.getDisplayCompName(webElement.getTag());
        ElementRenderer renderer = (ElementRenderer) renderers.get(displayComp);
        if (renderer != null) {
            //simple Element
            return renderer.getValue();
        }
        if (!hasCompositePanel()) return webElement.getTag();
        CompositeElementPanelFactory factory =
                getCompositePanelFactory(displayComp);
        if (factory != null) {
            return factory.getDisplay();
        }
        return webElement.getTag();
    }

    public boolean hasTableRenderer() {
        Enumeration keys = renderers.keys();
        while (keys.hasMoreElements()) {
            if (renderers.get(keys.nextElement()) instanceof TableRenderer) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the value of individualTab.
     *
     * @return  the value of individualTab.
     * @see #setIndividualTab(boolean)
     */
    public boolean isIndividualTab() {
        return individualTab;
    }

    /**
     * Sets the specified boolean to this individualTab.
     *
     * @param     a value of this individualTab.
     * @see #isIndividualTab()
     */
    public void setIndividualTab(boolean individualTab) {
        this.individualTab = individualTab;
    }

    /**
     * Gets the all reqired elements are not empty or not.
     *
     * @return  <code>true</code> if all reqired elements are not empty;
     * <code>false</code> otherwise.
     */
    protected boolean requiredNotEmpty() {
        Enumeration en = renderers.keys();
        while (en.hasMoreElements()) {
            String name = (String) en.nextElement();
            ElementRenderer renderer = (ElementRenderer) renderers.get(name);
            try {
                if (webElement.isChildRequired(name)
                        && renderer.isContentEmpty()
                        && renderer.getComponent().isEnabled()) {
                    emptyChildFactory = null;
                    emptyChild = name;
                    return false;
                }
            } catch (DeploymentException e) {
            }
        }
        Enumeration cfs = getCompositePanelFactories();
        while (cfs.hasMoreElements()) {
            CompositeElementPanelFactory factory =
                    (CompositeElementPanelFactory) cfs.nextElement();
            boolean neededToCheck = factory.isRequired()
                    || factory instanceof UnboundedCompositeElementPanelFactory
                    || factory.getActive();
            if (neededToCheck && !factory.requiredNotEmpty()) {
                emptyChildFactory = factory;
                return false;
            }
        }
        return true;
    }

    /**
     * Gets the empty child name.
     *
     * @return  the empty child name.
     */
    protected String getEmptyChild() {
        if (emptyChildFactory != null) {
            return emptyChildFactory.getEmptyChild();
        }
        return emptyChild;
    }

    /**
     * Goes to necessary tab.
     * In case of simple child empty:
     * if factory has only simple elements - we do nothing.
     * if factory has some tabs we have to goto basics tab.
     * In case of composite child has empty simple child:
     * we goes to composite child tab.
     */
    protected void gotoEmpty() {
        if (emptyChildFactory == null || !emptyChildFactory.isIndividualTab()) {
            if (createPanel() instanceof JTabbedPane) {
                int basicsIndex =
                        ((JTabbedPane) createPanel()).indexOfTab(EditorPanel.BASICS);
                ((JTabbedPane) createPanel()).setSelectedIndex(basicsIndex);
            }
            emptyRequestFocus();
        } else {
            try {
                ((JTabbedPane) createPanel()).
                        setSelectedComponent(emptyChildFactory.createPanel());
                emptyChildFactory.gotoEmpty();
            } catch (Exception e) {
                System.out.println("cast exception when goto " + e);
            }
        }
    }

    /**
     * Empty element renderer requests the focus.
     */
    protected void emptyRequestFocus() {
        if (emptyChildFactory == null) { // empty - simple element
            ElementRenderer rend = (ElementRenderer) renderers.get(emptyChild);
            if (rend != null) {
                rend.getComponent().requestFocus();
            }
        } else {
            emptyChildFactory.emptyRequestFocus();
        }
    }
}