package com.enc.pvpcod.module;

import net.minecraft.world.item.Items;

/** Friends, name icons and the wardrobe (skin + cape). */
public final class SocialModules {

    public static final class Friends extends Module {
        public final Setting.Bool colourNames = bool("colour", "Colour friends' names", true);
        public final Setting.Color nameColour = color("namecolour", "Friend name colour", 0xFF41D97A);
        public final Setting.Bool noHit = bool("nohit", "Don't let me hit friends", true);
        public final Setting.Bool heart = bool("heart", "Heart icon next to friends", false);

        public Friends() {
            super("friends", "Friends", Category.PVP, Items.POPPY, true,
                    "Your friends list: coloured names, a heart if you like, and no accidental hits.");
        }

        @Override
        public boolean hasCustomScreen() {
            return true;
        }
    }

    public static final class NameIcons extends Module {
        public final Setting.Choice who = choice("who", "Show the icon on", 1, "Just me", "Me and friends", "Everyone");
        public final Setting.Bool nametags = bool("nametags", "Above heads", true);
        public final Setting.Bool tab = bool("tab", "In the tab list", true);

        public NameIcons() {
            super("nameicons", "Cod icons", Category.COSMETIC, Items.COD_BUCKET, true,
                    "A little cod next to names. The admin panel can give players other icons.");
        }
    }

    public static final class Skin extends Module {
        public final Setting.Text file = text("file", "");
        public final Setting.Bool slim = bool("slim", "Slim arms", false);

        public Skin() {
            super("skin", "Skin swapper", Category.COSMETIC, Items.PLAYER_HEAD, false,
                    "Wear any skin file on your own screen.");
        }

        @Override
        public boolean hasCustomScreen() {
            return true;
        }
    }

    public static final class Cape extends Module {
        public final Setting.Text cape = text("cape", "builtin:cod_orange");
        public final Setting.Bool elytra = bool("elytra", "Use it on the elytra too", true);
        public final Setting.Bool cloth = bool("cloth", "Cloth capes (capes flow and wave)", true);
        public final Setting.Num clothWave = num("clothwave", "Cloth wave", 0, 3, 0.1f, 1, "x");
        public final Setting.Bool clothAll = bool("clothall", "Cloth on everyone's capes", true);

        public Cape() {
            super("cape", "Cape swapper", Category.COSMETIC, Items.WHITE_BANNER, false,
                    "Pick a cod cape or add your own cape files.");
        }

        @Override
        public boolean hasCustomScreen() {
            return true;
        }
    }

    private SocialModules() {
    }
}
