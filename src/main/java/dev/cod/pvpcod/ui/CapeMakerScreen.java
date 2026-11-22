package dev.cod.pvpcod.ui;

import dev.cod.packcod.ui.PvpTheme;
import dev.cod.pvpcod.Wardrobe;
import dev.cod.pvpcod.module.Modules;
import dev.cod.pvpcod.module.Setting;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Make a cape from any picture: upload (or drag it onto the window), drag the crop box
 * to pick the part you want, scroll or use the slider to resize it, then save.
 * The cape is saved as a normal 64x32 (or HD) cape png in config/pvpcod/capes.
 */
public final class CapeMakerScreen extends UiScreen {

    /** The back of a cape is 10 wide by 16 tall. */
    private static final float ASPECT = 10f / 16f;

    private int[] src;           // ARGB, row by row
    private int srcW, srcH;
    private Identifier srcTex;   // shrunk copy for showing on screen
    private int shownW, shownH;  // size of srcTex

    // crop box in source pixels: centre and height (width follows the cape's shape)
    private float cropX, cropY, cropH;

    private final Setting.Num zoom = new Setting.Num("zoom", "Crop size", 5, 100, 1, 100, "%");
    private final Setting.Choice quality = new Setting.Choice("q", "Quality", 1, "64x32", "128x64 (HD)", "256x128 (HD)");
    private final Setting.Choice elytra = new Setting.Choice("e", "Elytra", 0, "Picture", "Edge colour");
    private final Setting.Choice inside = new Setting.Choice("i", "Inside", 0, "Darker picture", "Edge colour");
    private final Setting.Color edge = new Setting.Color("edge", "Edge colour", 0);

    private NativeImage out;
    private DynamicTexture outTex;
    private Identifier outId;
    private boolean dirty = true;

    private EditBox name;
    private String nameText = "";
    private String status = "Upload a picture, or drag one onto this window";
    private int statusColor;
    private boolean draggingCrop;
    private int imgX, imgY, imgW, imgH; // where the picture is drawn this frame

    public CapeMakerScreen(Screen parent) {
        super("Cape maker", parent);
        statusColor = PvpTheme.dim();
    }

    @Override
    protected void init() {
        name = new EditBox(font, 0, 0, 110, 10, Component.literal("Name"));
        searchBox(name);
        name.setMaxLength(32);
        name.setHint(Component.literal("Cape name"));
        name.setValue(nameText);
        name.setResponder(v -> nameText = v);
        addRenderableWidget(name);
    }

    // ------------------------------------------------------------------ loading

    private void pick() {
        Thread t = new Thread(() -> {
            String path;
            try (org.lwjgl.system.MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
                org.lwjgl.PointerBuffer filters = stack.mallocPointer(5);
                for (String f : new String[]{"*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"}) filters.put(stack.UTF8(f));
                filters.flip();
                path = org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_openFileDialog("Pick a picture for your cape", null, filters, "Pictures", false);
            } catch (Throwable e) {
                minecraft.execute(() -> say("Couldn't open the file picker. Drag the picture onto this window instead.", PvpTheme.bad()));
                return;
            }
            if (path != null) {
                String p = path;
                minecraft.execute(() -> load(Path.of(p)));
            }
        }, "pvpcod-cape-picker");
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void onFilesDrop(List<Path> paths) {
        if (!paths.isEmpty()) load(paths.get(0));
    }

    private void load(Path p) {
        BufferedImage img;
        try {
            img = javax.imageio.ImageIO.read(p.toFile());
        } catch (Exception e) {
            img = null;
        }
        if (img == null) {
            say("That file isn't a picture I can read (try PNG or JPG)", PvpTheme.bad());
            return;
        }
        srcW = img.getWidth();
        srcH = img.getHeight();
        src = img.getRGB(0, 0, srcW, srcH, null, 0, srcW);
        // a shrunk copy to show on screen
        if (srcTex != null) minecraft.getTextureManager().release(srcTex);
        float s = Math.min(1f, 512f / Math.max(srcW, srcH));
        shownW = Math.max(1, Math.round(srcW * s));
        shownH = Math.max(1, Math.round(srcH * s));
        NativeImage shown = new NativeImage(shownW, shownH, true);
        for (int y = 0; y < shownH; y++)
            for (int x = 0; x < shownW; x++)
                shown.setPixel(x, y, src[Math.min(srcH - 1, (int) (y / s)) * srcW + Math.min(srcW - 1, (int) (x / s))]);
        srcTex = Identifier.fromNamespaceAndPath("pvpcod", "cape_maker/source" + System.nanoTime());
        minecraft.getTextureManager().register(srcTex, new DynamicTexture(() -> "pvpcod cape source", shown));

        zoom.value = 100;
        cropX = srcW / 2f;
        cropY = srcH / 2f;
        fitCrop();
        edge.value = averageEdge();
        if (name.getValue().isEmpty()) name.setValue(p.getFileName().toString().replaceAll("\\.[A-Za-z]+$", ""));
        dirty = true;
        say("Drag the box to pick the part you want, scroll to resize", PvpTheme.good());
    }

    /** Biggest crop box of the cape's shape that fits, times the size slider. */
    private void fitCrop() {
        float maxH = Math.min(srcH, srcW / ASPECT);
        cropH = Math.max(4, maxH * zoom.value / 100f);
        clampCrop();
    }

    private void clampCrop() {
        float hw = cropH * ASPECT / 2, hh = cropH / 2;
        cropX = Math.max(hw, Math.min(srcW - hw, cropX));
        cropY = Math.max(hh, Math.min(srcH - hh, cropY));
    }

    private int averageEdge() {
        long r = 0, g = 0, b = 0, n = 0;
        for (int i = 0; i < src.length; i += Math.max(1, src.length / 4000)) {
            int c = src[i];
            if ((c >>> 24) < 0x80) continue;
            r += (c >> 16) & 0xFF;
            g += (c >> 8) & 0xFF;
            b += c & 0xFF;
            n++;
        }
        if (n == 0) return 0xFF333333;
        int c = 0xFF000000 | ((int) (r / n) << 16) | ((int) (g / n) << 8) | (int) (b / n);
        return PvpTheme.mix(c, 0xFF000000, 0.35f);
    }

    // ------------------------------------------------------------------ making the cape

    private int scale() {
        return 1 << quality.index;
    }

    /** Average of the source pixels under one output pixel of a box (u0..u1, v0..v1 in 0..1 of the crop). */
    private int sample(float u0, float v0, float u1, float v1) {
        float cw = cropH * ASPECT, ch = cropH;
        float sx0 = cropX - cw / 2 + u0 * cw, sx1 = cropX - cw / 2 + u1 * cw;
        float sy0 = cropY - ch / 2 + v0 * ch, sy1 = cropY - ch / 2 + v1 * ch;
        int x0 = Math.max(0, (int) sx0), x1 = Math.min(srcW, Math.max(x0 + 1, (int) Math.ceil(sx1)));
        int y0 = Math.max(0, (int) sy0), y1 = Math.min(srcH, Math.max(y0 + 1, (int) Math.ceil(sy1)));
        long a = 0, r = 0, g = 0, b = 0, n = 0;
        int step = Math.max(1, Math.max(x1 - x0, y1 - y0) / 6);
        for (int y = y0; y < y1; y += step) {
            for (int x = x0; x < x1; x += step) {
                int c = src[y * srcW + x];
                a += c >>> 24;
                r += (c >> 16) & 0xFF;
                g += (c >> 8) & 0xFF;
                b += c & 0xFF;
                n++;
            }
        }
        if (n == 0) return edge.value;
        int col = ((int) (a / n) << 24) | ((int) (r / n) << 16) | ((int) (g / n) << 8) | (int) (b / n);
        // see-through parts of the picture show the edge colour (capes can't be see-through)
        float alpha = (col >>> 24) / 255f;
        return PvpTheme.mix(edge.value, col | 0xFF000000, alpha) | 0xFF000000;
    }

    /** Fills a w x h area at (x, y) of the sheet with the crop. */
    private void paint(NativeImage img, int x, int y, int w, int h, boolean darker, boolean mirror) {
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                int ii = mirror ? w - 1 - i : i;
                int c = sample(ii / (float) w, j / (float) h, (ii + 1) / (float) w, (j + 1) / (float) h);
                if (darker) c = PvpTheme.mix(c, 0xFF000000, 0.35f) | 0xFF000000;
                img.setPixel(x + i, y + j, c);
            }
        }
    }

    private void rebuild() {
        dirty = false;
        if (src == null) return;
        int s = scale();
        int w = 64 * s, h = 32 * s;
        if (out == null || out.getWidth() != w) {
            if (outId != null) minecraft.getTextureManager().release(outId);
            out = new NativeImage(w, h, true);
            outTex = new DynamicTexture(() -> "pvpcod cape maker", out);
            outId = Identifier.fromNamespaceAndPath("pvpcod", "cape_maker/out" + System.nanoTime());
            minecraft.getTextureManager().register(outId, outTex);
        }
        int e = edge.value | 0xFF000000;
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) out.setPixel(x, y, e);
        // the edges a shade lighter so the cape has an outline
        int light = PvpTheme.mix(e, 0xFFFFFFFF, 0.12f) | 0xFF000000;
        for (int x = s; x < 21 * s; x++) for (int y = 0; y < s; y++) out.setPixel(x, y, light);
        paint(out, s, s, 10 * s, 16 * s, false, false);                    // back (what people see)
        if (inside.index == 0) paint(out, 12 * s, s, 10 * s, 16 * s, true, true); // inside, facing you
        if (elytra.index == 0) {
            paint(out, 22 * s, 0, 24 * s, 22 * s, false, false);            // elytra wings
        }
        outTex.upload();
    }

    private void save() {
        if (src == null) {
            say("Upload a picture first", PvpTheme.bad());
            return;
        }
        if (dirty) rebuild();
        String n = name.getValue().trim().replaceAll("[^A-Za-z0-9 _-]", "").trim();
        if (n.isEmpty()) n = "my cape";
        try {
            Wardrobe.ensureFolders();
            Path file = Wardrobe.CAPES.resolve(n + ".png");
            int i = 2;
            while (Files.exists(file)) file = Wardrobe.CAPES.resolve(n + " " + (i++) + ".png");
            out.writeToFile(file);
            Modules.CAPE.cape.value = "file:" + file.getFileName();
            Modules.CAPE.setEnabled(true);
            Modules.save();
            Wardrobe.reload();
            say("Saved and put on: " + file.getFileName(), PvpTheme.good());
        } catch (Exception ex) {
            say("Couldn't save the cape: " + ex.getMessage(), PvpTheme.bad());
        }
    }

    private void say(String s, int c) {
        status = s;
        statusColor = c;
    }

    // ------------------------------------------------------------------ drawing

    @Override
    protected void draw(GuiGraphics g, int mx, int my) {
        if (dirty) rebuild();
        topBar(g, "Cape maker");
        String back = "Back";
        button(g, width - M - bw(back), 3, bw(back), 16, back, mx, my, false, b -> onClose());

        int y = TOP + M, h = height - y - M;
        int rw = Math.min(190, width / 3);
        int lw = width - M * 3 - rw;
        int lx = M, rx = lx + lw + M;

        // ---- left: the picture with the crop box
        PvpTheme.panel(g, lx, y, lw, h);
        String up = "Upload picture";
        button(g, lx + 8, y + 6, bw(up), 16, up, mx, my, true, b -> pick());
        g.drawString(font, PvpTheme.fit(font, status, lw - bw(up) - 24), lx + 16 + bw(up), y + 10, statusColor, false);
        int ax = lx + 8, ay = y + 28, aw = lw - 16, ah = h - 62;
        PvpTheme.slot(g, ax, ay, aw, ah);
        imgW = imgH = 0;
        if (srcTex != null) {
            float s = Math.min(aw / (float) srcW, ah / (float) srcH);
            imgW = Math.max(1, Math.round(srcW * s));
            imgH = Math.max(1, Math.round(srcH * s));
            imgX = ax + (aw - imgW) / 2;
            imgY = ay + (ah - imgH) / 2;
            g.blit(RenderPipelines.GUI_TEXTURED, srcTex, imgX, imgY, 0f, 0f, imgW, imgH, shownW, shownH, shownW, shownH);
            // darken outside the crop
            int cx0 = imgX + Math.round((cropX - cropH * ASPECT / 2) * s), cx1 = imgX + Math.round((cropX + cropH * ASPECT / 2) * s);
            int cy0 = imgY + Math.round((cropY - cropH / 2) * s), cy1 = imgY + Math.round((cropY + cropH / 2) * s);
            int shade = 0x99000000;
            g.fill(imgX, imgY, imgX + imgW, cy0, shade);
            g.fill(imgX, cy1, imgX + imgW, imgY + imgH, shade);
            g.fill(imgX, cy0, cx0, cy1, shade);
            g.fill(cx1, cy0, imgX + imgW, cy1, shade);
            PvpTheme.outline(g, cx0 - 1, cy0 - 1, cx1 - cx0 + 2, cy1 - cy0 + 2, PvpTheme.accent());
            // corner handles
            for (int[] c : new int[][]{{cx0, cy0}, {cx1, cy0}, {cx0, cy1}, {cx1, cy1}}) g.fill(c[0] - 2, c[1] - 2, c[0] + 2, c[1] + 2, PvpTheme.accent());
            wheels.add(new Wheel(ax, ay, aw, ah, d -> {
                zoom.set(zoom.value - d * 4);
                fitCrop();
                dirty = true;
            }));
        } else {
            String s1 = "No picture yet";
            g.drawString(font, s1, ax + (aw - font.width(s1)) / 2, ay + ah / 2 - 4, PvpTheme.faint(), false);
        }
        int sy = y + h - 26;
        g.drawString(font, zoom.label, lx + 8, sy + 1, PvpTheme.text(), false);
        int slx = lx + 8 + font.width(zoom.label) + 8;
        slider(g, slx, sy, lw - (slx - lx) - 50, zoom, mx, my, () -> {
            fitCrop();
            dirty = true;
        });
        g.drawString(font, zoom.shown(), lx + lw - 8 - font.width(zoom.shown()), sy + 1, PvpTheme.dim(), false);

        // ---- right: result and options
        PvpTheme.panel(g, rx, y, rw, h);
        PvpTheme.label(g, font, "Your cape", rx + 8, y + 8);
        int py = y + 22;
        int backW = 50, backH = 80;
        PvpTheme.slot(g, rx + 8, py, backW + 4, backH + 4);
        if (outId != null && src != null) {
            int s = scale();
            g.blit(RenderPipelines.GUI_TEXTURED, outId, rx + 10, py + 2, (float) s, (float) s, backW, backH, 10 * s, 16 * s, 64 * s, 32 * s);
            // the whole sheet, small
            int sw = rw - backW - 28, sh = sw / 2;
            PvpTheme.slot(g, rx + backW + 18, py, sw + 2, sh + 2);
            g.blit(RenderPipelines.GUI_TEXTURED, outId, rx + backW + 19, py + 1, 0f, 0f, sw, sh, 64 * s, 32 * s, 64 * s, 32 * s);
            g.drawString(font, "Whole sheet", rx + backW + 19, py + sh + 6, PvpTheme.faint(), false);
        }
        py += backH + 12;

        py = choiceRow(g, rx, py, rw, quality, mx, my);
        py = choiceRow(g, rx, py, rw, elytra, mx, my);
        py = choiceRow(g, rx, py, rw, inside, mx, my);
        g.drawString(font, edge.label, rx + 8, py + 4, PvpTheme.text(), false);
        swatch(g, rx + rw - 38, py + 1, 30, 12, edge.value);
        hits.add(new Hit(rx + rw - 38, py + 1, 30, 12, b -> {
            dirty = true;
            minecraft.setScreen(new ColorScreen(this, edge.label, edge));
        }, null));
        py += 20;

        name.setX(rx + 12);
        name.setY(y + h - 40);
        name.setWidth(rw - 24);
        PvpTheme.slot(g, rx + 8, y + h - 43, rw - 16, 15);
        button(g, rx + 8, y + h - 22, rw - 16, 16, "Save and wear", mx, my, true, src != null, b -> save());
    }

    private int choiceRow(GuiGraphics g, int x, int y, int w, Setting.Choice c, int mx, int my) {
        g.drawString(font, c.label, x + 8, y + 4, PvpTheme.text(), false);
        String v = c.get();
        int bw = Math.max(60, font.width(v) + 22);
        int bx = x + w - 8 - bw;
        PvpTheme.slot(g, bx, y, bw, 15);
        boolean hov = in(mx, my, bx, y, bw, 15);
        g.drawString(font, v, bx + (bw - font.width(v)) / 2, y + 4, hov ? PvpTheme.accent() : PvpTheme.text(), false);
        hits.add(new Hit(bx, y, bw, 15, b -> {
            c.next(b == 1 ? -1 : 1);
            dirty = true;
        }, null));
        return y + 19;
    }

    // ------------------------------------------------------------------ dragging the crop box

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (src != null && imgW > 0 && in(event.x(), event.y(), imgX, imgY, imgW, imgH) && !anyHit(event.x(), event.y())) {
            draggingCrop = true;
            moveCrop(event.x(), event.y());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean anyHit(double mx, double my) {
        for (Hit h : hits) if (h.in(mx, my)) return true;
        return false;
    }

    private void moveCrop(double mx, double my) {
        float s = imgW / (float) srcW;
        cropX = (float) ((mx - imgX) / s);
        cropY = (float) ((my - imgY) / s);
        clampCrop();
        dirty = true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggingCrop) {
            moveCrop(event.x(), event.y());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingCrop) {
            draggingCrop = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void onClose() {
        if (srcTex != null) minecraft.getTextureManager().release(srcTex);
        if (outId != null) minecraft.getTextureManager().release(outId);
        srcTex = outId = null;
        out = null;
        super.onClose();
    }
}
