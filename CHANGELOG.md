# Changelog: client.cod (pvp.cod)

Every update gets a new `0.x` version. The version also shows small in the mod's top bar.
Newest first.

## 0.8
- Pumpkin blur: removes the carved pumpkin overlay (option to hide the powder snow frost too).
- Boat map visibility: your map stays up on screen while you row a boat.
- Item scaling: make held items smaller or bigger and move them sideways, up/down, forward/back; both hands, or just main or off hand. Shrinks in place. (Side shield still controls the shield when it's on.)
- Riptide shield fix: your shield is hidden while you're spinning with Riptide.
- All four have the live view on their settings page in a world.
- New Media page in the side bar: what being media gives Creators and SMPs, and an Apply button (says "Coming soon" for now).

## 0.7
- Fixed: "14 of 42 on" no longer sits on top of Credits in the side bar (it's in the top bar now), and the side bar scrolls if the window is short.
- Credits: the flag next to 7kcod is now Belarus (red over green, with the white and red ornament).
- Live preview: in a world, the settings page of Block outline, Hitboxes, Fullbright, Clear water, Clear lava, Low fire, Motion blur, Custom crosshair, Aspect ratio and Clean HUD has a window onto your real game (marked LIVE), and the game keeps running behind it, so you see the real thing as you change settings. Outside a world the drawn preview is still there.
- Cloth capes: capes are drawn as 16 strips that bend and ripple as you move instead of a stiff board. On/off and "everyone's capes" in Wardrobe, wave strength in the cape settings (the ... next to it).
- Motion blur: strength 1 to 5, as a proper post effect (the HUD isn't blurred).
- Pause menu: the bobbing cod top left (same as the title screen) opens pvp.cod, instead of the "pvp.cod" box.

## 0.6
- Other players' capes now come from LabyMod and OptiFine only (MinecraftCapes removed). The module is called "LabyMod + OptiFine capes"; LabyMod is checked first.

## 0.5
- Block outline (Lunar style): colour, thickness, rainbow (chroma) with speed, and a see-through fill in its own colour. Preview on its settings page.
- Other clients' capes: players with no Minecraft cape show their OptiFine, MinecraftCapes or LabyMod cape (each can be turned off; "Reload capes" fetches again). Lunar Client cosmetics can't be shown: Lunar keeps them on its own private servers and only the Lunar launcher can get them.

## 0.4
- Team pings (Fortnite style): middle-click in survival to ping where you're looking (up to 256 blocks). A diamond marker shows the spot with the pinger's name, distance, coords and a small see-through head; it sticks to the screen edge when it's off screen and fades after 10 s. The coords go to every team member online as a /msg, so they're in their chat; team members with pvp.cod also get the marker. Pinging a player who isn't on your team or a friend marks them as an enemy (red). Middle click no longer picks blocks in survival while this is on (setting). Sneak + middle-click still adds friends.
- Team page: a team list separate from friends (side bar, or "Edit team" on the Team pings settings).
- Tier tagger: each player's MCTiers tier (like HT3) next to their name above their head and in tab. Pick which game mode to show (Sword, Axe, Pot, Neth Pot, SMP, UHC, Vanilla, Mace); if they're unranked in it, their best tier shows (setting). Tier colours by rank, retired tiers in italics.
- Admin panel has an Icons / Tiers switch. On Tiers, pick a player, pick a game mode, and click a tier (HT1 to LT5) to set it; "Clear" goes back to their real tier. The copy/paste code now carries tiers too.
- Cosmetics: halo, top hat, crown, cat ears, devil horns, shades, bandana, wings (flapping) and a cod on your head. Each one has its own on/off and colour, with a live preview of you on the settings page. Only you see them.
- Rescale aspect ratio (stretched res): 4:3, 5:4, 16:10, 1:1 or custom, with a preview.
- Shulker box tooltips: hover a shulker to see a 9x3 grid of what's inside, tinted with its colour.
- Armor status durability can show both the number and the percent.
- Credits: a little Lithuanian flag next to 7kcod.
- Custom crosshair, Totem pop counter, Clean HUD and Reach were already in (0.3 / 0.1).

## 0.3
- Skins: type any player's name in the Wardrobe and press Get (or Enter) to wear their skin. It downloads their current skin from Mojang, saves it in the skins folder and picks slim arms if their skin uses them.
- Credits page, same as pack.cod's (Made by 7kcod, solo dev, with your head), in the side bar.
- Low fire can go twice as low now (up to 1.2).
- New modules:
  - Custom crosshair: 6 styles, size, gap, thickness, colour, outline, and a different colour when you aim at a player (with a preview).
  - Target HUD: the face, health bar, absorption, distance and totem pops of whoever you last hit.
  - Totem pop counter: tells you in chat when someone pops a totem and how many they've popped (resets when they die).
  - Clean HUD: hide boss bars, the scoreboard or the effect icons.
  - Saturation, Memory and Biome info boxes.
- Capes: 20 built-in designs now, drawn in HD (128x64): stripes, checkers, stars, flames, diagonal, split, waves, border and fish scales, with emblems (cod, salmon, blaze powder, ender pearl, emerald, snowball, golden apple, totem, diamond sword, end crystal, pufferfish, nether star, a heart) taken from your pack. The old 10 keep their names with new looks.

## 0.2
- Previews on the settings page of Hitboxes (normal / looking at / friend, with eye line and look arrow), Low fire, Clear water, Clear lava, Side shield, Zoom, Fullbright, No hurt cam (Off | On side by side, updating as you move the sliders), and Cod icons / Friends (sample name tags).
- Every module has its own icon now (no repeats): Keystrokes lever, FPS repeater, Ping eye of ender, Potion effects brewing stand, Cod icons cod bucket.
- Cape maker: upload a picture (or drag it onto the window), drag the crop box, scroll or slide to resize it, choose quality (64x32 or HD 128x64 / 256x128), what the elytra and inside show, and the edge colour. Saves to config/pvpcod/capes and puts it on. Open it from Wardrobe or the side bar.
- pack.cod only shows up inside pvp.cod now: its cod button on the title screen and the Resource Packs screen is gone. Open it from the pvp.cod menu.
- The title screen button is the cod now (was a sword), top left.
- BUILD.bat also clears out a disabled pack-cod jar.

## 0.1
First version. A PvP client as a Fabric mod for 1.21.11, with pack.cod built in.

- Menu: Right Shift in game, the sword next to the cod on the title screen, or the pvp.cod button on the pause menu. Every module is a tile with an on/off switch; click a tile for its settings. Categories (HUD, Visual, PvP, Cosmetic) and search. Uses pack.cod's look (palette, accent and style from pack.cod's Look screen).
- Every setting is editable: sliders, switches, choices, and a colour picker (presets, hue/saturation/brightness/opacity, hex).
- Edit HUD: drag HUD boxes anywhere, scroll to resize, right-click for settings. Snaps to screen edges, the middle and other boxes (Shift = no snapping).
- HUD modules: Keystrokes (WASD, LMB/RMB with CPS, space; fade animation), FPS, CPS, Ping, Coordinates, Direction, Speed, Clock, Reach, Combo, Server, Armor status, Potion effects, Totem counter, Item counter (potions, gapples, pearls, crystals, arrows, cobwebs, obsidian).
- Visual: Low fire (how low, or hide), Clear water (fog distance, remove underwater tint), Clear lava (optionally only with Fire Resistance), Side shield (move/size the shield, keep while blocking), Hitboxes (box colour, colour for what you look at and for friends, eye line colour, look arrow), Zoom (C, smooth, smooth camera), Fullbright, No hurt cam.
- PvP: Friends (coloured names, heart icon, no hitting friends; add/remove in the Friends page, from the online list, or middle-click a player), Toggle sprint (always or toggle).
- Cosmetic: Cod icons next to names (above heads and in tab; on you, you + friends, or everyone), Skin swapper (skins folder, slim arms), Cape swapper (10 built-in cod capes drawn from your pack's cod texture, or your own cape files; on elytra too). Skin and cape only show on your screen.
- Admin panel: needs an admin key (you make it the first time). Pick the icon for each player (36 icons: fish, items, hearts) or change the normal icon for everyone else. Copy/paste an icon code so friends see the same icons.
- pack.cod 0.23 is part of the jar (built from ../pack-cod-src). The separate pack-cod jar is removed from mods when pvp.cod installs.
