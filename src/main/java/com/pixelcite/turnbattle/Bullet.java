package com.pixelcite.turnbattle;

public class Bullet {
   public float x;
   public float y;
   public float prevX;
   public float prevY;
   public float vx;
   public float vy;
   public float ax;
   public float ay;
   public float size = 4.0F;
   public boolean active = true;
   public boolean grazed = false;
   public int pendingTicks = 0;
   public int kind = 0;
   public int color = 16777215;
   public int behavior = 0;
   public int timer = 0;
   public int age = 0;
   public float p1;
   public float p2;
   public float p3;
   public float tx;
   public float ty;
   public int count = 0;
   public int kind2 = 0;
   public boolean noWarn = false;
   public boolean child = false;
   public int hue = 0;
}
