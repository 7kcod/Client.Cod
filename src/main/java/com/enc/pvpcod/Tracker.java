package com.enc.pvpcod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;

/** Clicks per second, reach, combo and speed, read by the HUD modules. */
public final class Tracker {

    private static final ArrayDeque<Long> LEFT = new ArrayDeque<>();
    private static final ArrayDeque<Long> RIGHT = new ArrayDeque<>();
    private static boolean wasLeft, wasRight;

    public static double lastReach = -1;
    /** The last thing you hit (for Target HUD). */
    public static Entity lastTargetEntity;
    public static long lastHitAt;
    public static int combo;
    private static int lastTarget = -1;
    private static int lastHurt;

    public static double speed;
    private static double px = Double.NaN, pz;

    /** Called every frame: catches every press of attack/use. */
    public static void frame(Minecraft mc) {
        boolean l = mc.options.keyAttack.isDown(), r = mc.options.keyUse.isDown();
        long now = System.currentTimeMillis();
        if (l && !wasLeft) LEFT.addLast(now);
        if (r && !wasRight) RIGHT.addLast(now);
        wasLeft = l;
        wasRight = r;
        while (!LEFT.isEmpty() && now - LEFT.peekFirst() > 1000) LEFT.pollFirst();
        while (!RIGHT.isEmpty() && now - RIGHT.peekFirst() > 1000) RIGHT.pollFirst();
    }

    public static int leftCps() {
        return LEFT.size();
    }

    public static int rightCps() {
        return RIGHT.size();
    }

    public static void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null) {
            lastTargetEntity = null;
            px = Double.NaN;
            combo = 0;
            return;
        }
        if (!Double.isNaN(px)) {
            double dx = p.getX() - px, dz = p.getZ() - pz;
            speed = speed * 0.6 + Math.sqrt(dx * dx + dz * dz) * 20 * 0.4;
        }
        px = p.getX();
        pz = p.getZ();
        // Getting hit ends your combo.
        if (p.hurtTime > lastHurt) combo = 0;
        lastHurt = p.hurtTime;
        if (combo > 0 && System.currentTimeMillis() - lastHitAt > 3000) combo = 0;
    }

    public static void onAttack(Minecraft mc, Entity target) {
        LocalPlayer p = mc.player;
        if (p == null || target == null) return;
        Vec3 eye = p.getEyePosition();
        AABB b = target.getBoundingBox();
        double cx = Math.max(b.minX, Math.min(eye.x, b.maxX));
        double cy = Math.max(b.minY, Math.min(eye.y, b.maxY));
        double cz = Math.max(b.minZ, Math.min(eye.z, b.maxZ));
        lastReach = eye.distanceTo(new Vec3(cx, cy, cz));
        long now = System.currentTimeMillis();
        combo = target.getId() == lastTarget && now - lastHitAt < 3000 ? combo + 1 : 1;
        lastTarget = target.getId();
        lastTargetEntity = target;
        lastHitAt = now;
    }

    private Tracker() {
    }
}
