package com.scy.cameralib.camera;

import java.io.Serializable;

/**
 * Preferably useful for photo album selection entities
 * Created by SCY on 2017/7/24 14:37.
 */

public class PhotoPath implements Serializable {
    private String path;//picture path
    private String week;//Picture date
    private String time;//Photo capture time
    private boolean isChoose;//if selected

    public boolean isChoose() {
        return isChoose;
    }

    public void setChoose(boolean choose) {
        isChoose = choose;
    }

    public String getWeek() {
        return week;
    }

    public void setWeek(String week) {
        this.week = week;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
