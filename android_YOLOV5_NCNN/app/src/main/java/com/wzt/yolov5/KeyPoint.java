package com.wzt.yolov5;

import android.graphics.Color;
import android.graphics.RectF;

import java.util.Random;

public class KeyPoint {
    public float x, y;

    public KeyPoint(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

}
