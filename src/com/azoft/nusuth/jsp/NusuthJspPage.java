package com.azoft.nusuth.jsp;

import javax.servlet.jsp.HttpJspPage;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 * This is an abstract class for jsp page.
 * @author vdgg, skilz
 * @version 1.3
 * @since Nusuth1.0
 */
public abstract class NusuthJspPage extends HttpServlet implements HttpJspPage {

    private ServletConfig config;
    protected CustomTagFactory _jsp_TagFactory;

    public NusuthJspPage() {
        super();
    }

    public ServletConfig getServletConfig() {
        return super.getServletConfig();
    }

    public String getServletInfo() {
        return "";
    }

    /**
     * Init method. Creates a custom tag factory and calls jspInit() method.
     * throws ServletException if any error occurs while initialize jsp page.
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        _jsp_TagFactory = ((com.azoft.nusuth.core.NusuthContext) getServletConfig().
                getServletContext()).getCustomTagFactory();
        jspInit();
    }

    public void destroy() {
        jspDestroy();
        super.destroy();
    }

    protected void service(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, java.io.IOException {
        _jspService(req, res);
    }

    /**
     * Init method. Do nothing.
     */
    public void jspInit() {
    }

    public void jspDestroy() {
    }

    public abstract void _jspService(HttpServletRequest req,
                                     HttpServletResponse res)
            throws ServletException, java.io.IOException;
}

