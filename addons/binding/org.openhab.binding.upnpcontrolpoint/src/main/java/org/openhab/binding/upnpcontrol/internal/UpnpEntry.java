package org.openhab.binding.upnpcontrol.internal;

import org.apache.commons.lang.StringEscapeUtils;

public class UpnpEntry {

    private final String id;
    private final String title;
    private final String parentId;
    private final String upnpClass;
    private final String res;
    private final String album;
    private final String albumArtUri;
    private final String creator;
    private final int originalTrackNumber;
    private String desc;

    public UpnpEntry(String id, String title, String parentId, String album, String albumArtUri, String creator,
            String upnpClass, String res) {
        this(id, title, parentId, album, albumArtUri, creator, upnpClass, res, -1);
    }

    public UpnpEntry(String id, String title, String parentId, String album, String albumArtUri, String creator,
            String upnpClass, String res, int originalTrackNumber) {
        this.id = id;
        this.title = title;
        this.parentId = parentId;
        this.album = album;
        this.albumArtUri = albumArtUri;
        this.creator = creator;
        this.upnpClass = upnpClass;
        this.res = res;
        this.originalTrackNumber = originalTrackNumber;
        this.desc = null;
    }

    /**
     * @return the title of the entry.
     */
    @Override
    public String toString() {
        return title;
    }

    /**
     * @return the unique identifier of this entry.
     */
    public String getId() {
        return id;
    }

    /**
     * @return the title of the entry.
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return the unique identifier of the parent of this entry.
     */
    public String getParentId() {
        return parentId;
    }

    /**
     * @return a URI of this entry.
     */
    public String getRes() {
        return res;
    }

    /**
     * @return the UPnP classname for this entry.
     */
    public String getUpnpClass() {
        return upnpClass;
    }

    /**
     * @return the name of the album.
     */
    public String getAlbum() {
        return album;
    }

    /**
     * @return the URI for the album art.
     */
    public String getAlbumArtUri() {
        return StringEscapeUtils.unescapeXml(albumArtUri);
    }

    /**
     * @return the name of the artist who created the entry.
     */
    public String getCreator() {
        return creator;
    }

    public int getOriginalTrackNumber() {
        return originalTrackNumber;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

}
