package com.azoft.nusuth.container;

import com.azoft.nusuth.core.NusuthContext;
import com.azoft.nusuth.core.ResourceChain;
import com.azoft.nusuth.util.StrBuffer;

/**
 * Container invocation cache element.
 * Creation date: (21.11.00 21:29:20)
 * @author: IgorK (igork@novosoft.ru)
 */
public class ContainerInvocationCacheElement extends InvocationCacheElement {
    public NusuthContext context;
    public StrBuffer contextPath;
    public StrBuffer servletPath;
    public StrBuffer pathInfo;
    public String servletName;
    public ResourceChain chain;
}
