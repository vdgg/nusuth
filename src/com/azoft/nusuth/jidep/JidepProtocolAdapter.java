package com.azoft.nusuth.jidep;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.math.BigInteger;

import com.azoft.nusuth.core.*;
import com.azoft.nusuth.util.*;

/**
 * This class is responsible for adapt requests and responses to JIDEP protocol.
 * @author skilz
 * @since Nusuth1.0
 * @version 1.8
 */
public class JidepProtocolAdapter
        implements ClientJidepAdapter, ServerJidepAdapter, FlushListener {

    private Socket socket;
    private HttpNusuthServletInputStream is
            = new HttpNusuthServletInputStream(null);
    private ChunkedServletInputStream chunkedIs
            = new ChunkedServletInputStream(null);
    private FixedLengthServletInputStream fixedIs
            = new FixedLengthServletInputStream(null);
    private NusuthServletOutputStream os = new NusuthServletOutputStream(null);
    private org.apache.log4j.Category cat
            = org.apache.log4j.Category.getInstance("com.azoft.nusuth.jidep");
    private String reqcommand = null;
    private StrBuffer respProtocol = new StrBuffer();
    private StrBuffer code = new StrBuffer();
    private StrBuffer command = new StrBuffer();
    private StrBuffer preCommand = new StrBuffer();
    private StrBuffer protocol = new StrBuffer();
    private boolean isServer;
    private boolean chunked = true;
    private int respCode;
    private StrBuffer requestLine = new StrBuffer();
    private static byte[] systemCRLF;
    private static byte[] jidepProtocolWithSpace = " JIDEP/1.0".getBytes();
    private static byte[] jidepProtocol = "JIDEP/1.0".getBytes();
    private static byte[] jidepProtocolWithRightSpace = "JIDEP/1.0 ".getBytes();
    private static byte[] cmd = "cmd: ".getBytes();
    private static byte[] key = null;
    private final static byte[] LAST_CHUNK = {(byte) '0', 13, 10};
    private NusuthHeaders headers = new NusuthHeaders();
    private JidepSession session = new JidepSession();
    private boolean isAuthenticated = false;
    private boolean isLogged = false;


    static {
        if (File.pathSeparatorChar == ':') {
            systemCRLF = new byte[1];
            systemCRLF[0] = (byte) '\n';
        } else {
            systemCRLF = new byte[2];
            systemCRLF[0] = (byte) '\r';
            systemCRLF[1] = (byte) '\n';
        }
    }

    protected JidepProtocolAdapter(Socket socket, boolean isServer, String key) {
        this.isServer = isServer;
        this.socket = socket;
        this.key = key.getBytes();
        os.setFlushListener(this);
        init();
    }

    public void init() {
        respCode = 200;
        try {
            os.init(socket.getOutputStream());
            is.init(socket.getInputStream());
            os.setFlushListener(this);
            os.setChunked(true);
        } catch (IOException e) {
            cat.error("Cannot init protocol adapter");
        }
    }

    public static JidepProtocolAdapter getClientSide(Socket socket, String key) {
        return new JidepProtocolAdapter(socket, false, key);
    }

    public static JidepProtocolAdapter getServerSide(Socket socket) {
        return new JidepProtocolAdapter(socket, true,
                JidepConnectionFactory.getKey());
    }

    public OutputStream getOutputStream() {
        return os;
    }

    public void setCommand(String command) {
        this.reqcommand = command;
    }

    public void endRequest() throws IOException {
        is.cleanup();
        is.init(socket.getInputStream());
        headers.clear();
        chunked = os.isCommited();
        if (!chunked) {
            os.setChunked(false);
            headers.putHeader("len", String.valueOf(os.getBytesInBuffer()));
        } else {
            headers.clearHeader("len");
        }
        os.close();
        os.cleanup();
        os.init(socket.getOutputStream());
        os.setChunked(true);
        os.setFlushListener(this);
    }

    public String getCommand() {
        return command.toString();
    }

    public void setStatus(int status) {
        respCode = status;
    }

    public void endResponse() throws IOException {
        synchronized (os) {
            headers.clear();
            chunked = os.isCommited();
            if (!chunked) {
                os.setChunked(false);
                headers.putHeader("len", String.valueOf(os.getBytesInBuffer()));
            } else {
                headers.clearHeader("len");
            }
            os.close();
            os.cleanup();
            os.init(socket.getOutputStream());
            os.setChunked(true);
            os.setFlushListener(this);
        }
    }

    /**
     * This method return content of the request.
     * @return content of thre request.
     */
    public InputStream getInputStream() {
        return retreiveInputStream();
    }

    /**
     * This method cleaning up adapter.
     */
    public void cleanup() {
        fixedIs.cleanup();
        chunkedIs.cleanup();
        code.clear();
        respProtocol.clear();
        protocol.clear();
        command.clear();
        preCommand.clear();
        reqcommand = null;
        chunked = true;
        requestLine.clear();
        headers.clear();
        respCode = 200;
    }

    public void parseRequest() throws IOException {
        parseRequestLine();
        headers.clear();
        headers.read(is);
    }

    public int getResponseCode() {
        return Integer.parseInt(code.toString());
    }

    public void parseResponse() throws IOException {
        synchronized (is) {
            cleanup();
            int numspaces = 0;
            int b;
            char c;
            do {
                b = is.read();
                if (b == -1) throw new IOException("Server closed socket");
                switch (c = (char) b) {
                    case '\r':
                        break;
                    case '\n':
                        break;
                    case ' ':
                        numspaces++;
                        if (numspaces > 1) throw new IOException("To many spaces detected");
                        break;
                    default:
                        switch (numspaces) {
                            case 0:
                                respProtocol.append(c);
                                break;
                            case 1:
                                code.append(c);
                                break;
                        }
                }
            } while (c != '\n');
            headers.clear();
            headers.read(is);
        }
    }

    private void parseRequestLine() throws IOException {
        int numspaces = 0;
        int b;
        char c;
        do {
            b = is.read();
            if (b == -1) throw new SocketException("Client closed socket");
            switch (c = (char) b) {
                case '\r':
                    break;
                case '\n':
                    break;
                case ' ':
                    numspaces++;
                    if (numspaces > 2) throw new IOException("To many spaces detected");
                    break;
                default :
                    switch (numspaces) {
                        case 0:
                            preCommand.append(c);
                            break;
                        case 1:
                            command.append(c);
                            break;
                        case 2:
                            protocol.append(c);
                    }
            }
        } while (c != '\n');
    }

    /**This method is called on Flush.
     * It writes request headers to the output stream.
     *@exception IOException Throws if any errors occures during flushing.
     */
    public void onFlush() throws IOException {
        if (!isServer) {
            os.write(cmd);
            os.write(reqcommand.getBytes());
            os.write(jidepProtocolWithSpace);
        } else {
            os.write(jidepProtocolWithRightSpace);
            os.write(String.valueOf(respCode).getBytes());
        }
        os.write(systemCRLF);
        headers.write(os);
    }

    /**
     * This method return FixedLengthServletInputStream if headers contains
     * <i>len</i> header, or ChunkedServletInputStream otherwise
     * @return HttpNusuthServletInputStream input stream.
     */
    private HttpNusuthServletInputStream retreiveInputStream() {
        if (headers.containsHeader("len")) {
            chunked = false;
            fixedIs.cleanup();
            fixedIs.init(is, headers.getIntHeader("len"));
            headers.clear();
            return fixedIs;
        } else {
            chunked = true;
            chunkedIs.cleanup();
            chunkedIs.init(is);
            headers.clear();
            return chunkedIs;
        }
    }

    public void close() throws IOException {
        socket.close();
    }

    /**
     * Process authentication.
     * @param key Authentication key.
     */
    public void processAuthenticate() throws IOException {
        setCommand("getpublickey");
        endRequest();
        parseResponse();
        InputStream ois = getInputStream();
        BigInteger e = null;
        BigInteger n = null;
        BigInteger d = null;
        int eLen = ois.read();
        byte[] eArr = new byte[eLen];
        ois.read(eArr);
        e = new BigInteger(eArr);
        int nLen = ois.read();
        byte[] nArr = new byte[nLen];
        ois.read(nArr);
        n = new BigInteger(nArr);
        RsaUtil rsa = new RsaUtil(e, n);
        cleanup();
        setCommand("authenticate");
        OutputStream os = getOutputStream();
        byte[] message = rsa.encrypt(new BigInteger(key)).toByteArray();
        os.write(message.length);
        os.write(message);
        endRequest();
        parseResponse();
        if (getResponseCode() == 200) {
            session.setAttribute("authenticated", new Boolean("true"));
        }
    }

    /**
     * This method sets the header.
     * @param name Header name.
     * @param value Header value
     */
    public void setHeader(String name, String value) {
        if (name.equals("len")) {
            os.setChunked(false);
            chunked = false;
        }
        headers.putHeader(name, value);
    }

    /**
     * This method return Jidep session.
     * @return Jidep session.
     */
    public JidepSession getSession() {
        return session;
    }

}
