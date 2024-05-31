package com.azoft.nusuth.distributor.cache;

class EvictQueue {
    /* freshly element */
    private CachedElement head;

    /* element to unload */
    private CachedElement memTail;

    /* element to delete from disk */
    private CachedElement diskTail;

    void touch(CachedElement elem) {
        remove(elem);
        add(elem);
    }

    /* adds new page to queue */
    void add(CachedElement elem) {
        if (head != null) {
            head.prev = elem;
            elem.next = head;
            elem.prev = null;
            head = elem;
            if (memTail == null)
                memTail = elem;
        } else {
            head = elem;
            memTail = elem;
            diskTail = elem;
            elem.prev = null;
            elem.next = null;
        }
    }

    /* removes page from queue */
    void remove(CachedElement elem) {
        if (elem.next != null)
            elem.next.prev = elem.prev;
        else {
            if (memTail == elem)
                memTail = elem.prev;
            if (diskTail == elem)
                diskTail = elem.prev;
        }

        if (elem.prev != null)
            elem.prev.next = elem.next;
        else
            head = elem.next;
    }

    CachedElement unload() {
        CachedElement elem = memTail;
        if (elem != null)
            memTail = elem.prev;

        return elem;
    }

    CachedElement remove() {
        CachedElement elem = diskTail;
        if (elem != null)
            remove(elem);

        return elem;
    }

    void clear() {
        diskTail = head = memTail = null;
    }
}

