package com.azoft.nusuth.core;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;


/**
 * This servlet is responsible for processing errors which occures while
 * processing client request.
 * @author skilz, vdgg.
 * @version 1.15
 * @since Nusuth1.0
 */
public class ErrorServlet extends HttpServlet {

    static HttpErrors httpErrors;

    static {
        httpErrors = new HttpErrors();
    }

    /**
     *  This method processing error occured while processing requested resource.
     * @param request HttpRequest.
     * @response HttpResponse.
     */
    public void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String additionalMessage = "";
        if (request.getAttribute("javax.servlet.error.additional") != null) {
            additionalMessage = "Cannot process error by \""
                    + request.getRequestURI() + "\", as since, a error occured there "
                    + "or it cannot be found.";
        }
        if (request.getMethod().equals("HEAD")) {
            HttpNusuthResponse resp = getRealResponse(response);
            resp.setBodySend(false);
        }
        boolean[] swu = {true, false};
        boolean[] swuold = {false, true};
        boolean writerUsed = false;
        HttpNusuthResponse resp = getRealResponse(response);
        if (resp.isWriterUsed()) {
            writerUsed = true;
            resp.getWriter().flush();
            resp.setStreamWriterUsage(swu);
        }
//    response.setContentType("text/html");
        resp.setContentType("text/html");
        Throwable ex = (Throwable) request.getAttribute("javax.servlet."
                + "error.exception");
        while (ex instanceof ServletException
                && ((ServletException) ex).getRootCause() != null) {
            ex = ((ServletException) ex).getRootCause();
        }
        int errorCode = ((Integer) request.
                getAttribute("javax.servlet.error.status_code")).intValue();
//      response.setStatus(errorCode);
        resp.setStatus(errorCode);
        String message
                = (String) request.getAttribute("javax.servlet.error.message");
        if (ex == null) {
            message = (message == null) ? "" : message;
            try {
//          ServletOutputStream out = response.getOutputStream();
                ServletOutputStream out = resp.getOutputStream();
                out.println("<H1>" + errorCode + " "
                        + httpErrors.getErrorDescription(errorCode) + "</H1>");
                out.println("<pre>" + message + "</pre>");
                out.println("<BR>");
                out.println(additionalMessage);
            } catch (IOException e) {
            }
        } else if (errorCode == 500) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            ex.printStackTrace(ps);
            ps.flush();
            String rawExc = baos.toString();
            int nestIndexFrom = 0;
            int nestIndexTo = rawExc.indexOf("nested:");
            String resExc = "";
            while (nestIndexTo >= 0) {
                resExc += (rawExc.substring(nestIndexFrom, nestIndexTo + 7) + "\r\n");
                nestIndexFrom = nestIndexTo + 8;
                if (nestIndexFrom >= rawExc.length()) {
                    break;
                }
                nestIndexTo = rawExc.indexOf("nested:", nestIndexFrom);
                if (nestIndexTo < 0) {
                    resExc += rawExc.substring(nestIndexFrom, rawExc.length());
                }
            }
            if (resExc.length() == 0) {
                resExc = rawExc;
            }
            try {
//          ServletOutputStream out = response.getOutputStream();
                ServletOutputStream out = resp.getOutputStream();
                out.println("<H1>" + errorCode + " "
                        + httpErrors.getErrorDescription(errorCode) + "</H1>");
                out.println("<pre>" + message + "</pre>");
                out.println("<br>");
                out.println("<pre>");
                out.print(resExc);
//          out.print(rawExc);
                out.println("</pre>");
                out.println(additionalMessage);
            } catch (IOException e) {
            }
        } else {
            try {
//          ServletOutputStream out = response.getOutputStream();
                ServletOutputStream out = resp.getOutputStream();
                out.println("<H1>" + errorCode + " "
                        + httpErrors.getErrorDescription(errorCode) + "</H1>");
                out.println("<pre>" + message + "</pre>");
                out.println("<br>");
                out.println(additionalMessage);
            } catch (IOException e) {
            }
        }
        if (writerUsed) {
            resp.setStreamWriterUsage(swuold);
        }
//    resp.close();
    }

    /**
     * This method return "real" response from given. If given response is
     * wrapper, then this method get response from it.
     * @param response HTTP response or wrapper;
     * @return HttpNusuthResponse Real HTTP response.
     */
    public HttpNusuthResponse getRealResponse(HttpServletResponse response) {
        HttpServletResponse resp = response;
        while (resp instanceof HttpServletResponseWrapper) {
            resp = (HttpServletResponse)
                    ((HttpServletResponseWrapper) resp).getResponse();
        }
        return (HttpNusuthResponse) resp;
    }

}

