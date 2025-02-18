package com.carsaiplay.pro.models;

import android.text.TextUtils;
import android.webkit.URLUtil;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class MediaItem {
    private final String url;
    private String title;
    private String type;
    private long size;
    private String quality;
    private String mimeType;
    private boolean isPlaylist;
    private boolean isProcessed;

    public MediaItem(String url) {
        this.url = url;
        this.title = extractTitleFromUrl(url);
        this.isProcessed = false;
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (!TextUtils.isEmpty(title)) {
            this.title = title;
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getQuality() {
        return quality;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public boolean isPlaylist() {
        return isPlaylist;
    }

    public void setPlaylist(boolean playlist) {
        isPlaylist = playlist;
    }

    public boolean isProcessed() {
        return isProcessed;
    }

    public void setProcessed(boolean processed) {
        isProcessed = processed;
    }

    private String extractTitleFromUrl(String url) {
        try {
            String fileName = URLUtil.guessFileName(url, null, null);
            if (TextUtils.isEmpty(fileName)) {
                fileName = url.substring(url.lastIndexOf('/') + 1);
                if (fileName.contains("?")) {
                    fileName = fileName.substring(0, fileName.indexOf("?"));
                }
            }
            return URLDecoder.decode(fileName, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return "Media_" + System.currentTimeMillis();
        }
    }

    public String getFormattedSize() {
        if (size <= 0) return "Desconhecido";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MediaItem mediaItem = (MediaItem) o;
        return url.equals(mediaItem.url);
    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }
}