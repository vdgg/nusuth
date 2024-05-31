package com.azoft.nusuth.jsp;

import java.beans.*;
import java.lang.reflect.Method;
import javax.servlet.jsp.JspException;
import javax.servlet.ServletRequest;

public class JspBeanUtils {

    public static Object processGetProperty(String beanName, String propName, Object bean)
            throws Exception {
        if (bean == null) {
            throw new Exception("Cannot find bean " + beanName);
        }
        Object value = null;
        Class beanClass = bean.getClass();
        BeanInfo bInfo = Introspector.getBeanInfo(beanClass);
        if (bInfo == null) {
            throw new Exception("No BeanInfo for the bean of type " + beanClass.getName() + " could be found, the class likely does not exist.");
        }
        PropertyDescriptor[] pds = bInfo.getPropertyDescriptors();
        Method getMethod = null;
        Class type = null;
        for (int i = 0; i < pds.length; i++) {
            if (pds[i].getName().equals(propName)) {
                getMethod = pds[i].getReadMethod();
                type = pds[i].getPropertyType();
            }
        }
        if (getMethod == null) {
            if (type == null) {
                throw new Exception("Cannot find property " + propName);
            } else {
                throw new Exception("Cannot find getter method for property " + propName);
            }
        }
        value = getMethod.invoke(bean, null);
        return value;
    }

}