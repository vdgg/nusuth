package com.azoft.nusuth.webappsecurity;

import com.azoft.nusuth.util.StrBuffer;

public class ResourceSecurityRecord {
    private StrBuffer path2Resource;
    private StrBuffer method;

    public ResourceSecurityRecord(StrBuffer path2Resource, StrBuffer method) {
        this.path2Resource = path2Resource;
        this.method = method;
    }

    public ResourceSecurityRecord(String path2Resource, String method) {
        this.path2Resource = new StrBuffer(path2Resource.length());
        this.path2Resource.append(path2Resource);
        this.method = new StrBuffer(method.length());
        this.method.append(method);
    }

    public StrBuffer getPath2Resource() {
        return path2Resource;
    }

    public StrBuffer getMethod() {
        return method;
    }
}

