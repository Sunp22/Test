package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class ShopScreen extends Screen {
   private static final int VW = 320;
   private static final int VH = 240;
   private float s = 1.0F;
   private float ox;
   private float oy;
   private static final int WHITE = -1;
   private static final int GREY = -8355712;
   private static final int YELLOW = -256;
   private static final int BLACK = -16777216;
   private static final Identifier DEFAULT_SOUL = Identifier.of("turnbattle", "textures/souls/redsoulsimple.png");
   private final int entityId;
   private int dd;
   private List<ShopScreen.CWare> wares;
   private final String greeting;
   private final long restockAtMs;
   private ShopScreen.Mode mode = ShopScreen.Mode.MENU;
   private int menuSel = 0;
   private int buySel = 0;
   private int buyScroll = 0;
   private int confirmSel = 0;
   private int talkSel = 0;
   private int tick = 0;
   private int leaveAt = -1;
   private String rightMsg = null;
   private int rightMsgUntil = 0;
   private boolean waiting = false;
   private float panelSlide = 0.0F;
   private long lastFrameNanos = 0L;
   private float now = 0.0F;
   private String typed = "";
   private float shown = 0.0F;
   private static final String[] MENU = new String[]{"Buy", "Sell", "Talk", "Exit"};
   private static final String[] TOPICS = new String[]{"About yourself", "About this place", "About D$", "Your stock", "Exit"};
   private static final int LIST_ROWS = 8;
   private static final int ROW_H = 13;
   private static final String[] SELL_LINES = new String[]{
      "Selling? Ha! Not today, friend. I've got all the junk I can carry.",
      "I'm a seller, not a buyer! Keep your things, I've no room for them.",
      "Sell to ME? You'd want D$ for that, and I'd rather keep mine."
   };
   private static final String[] BYE_LINES = new String[]{
      "Come back any time! Bring D$!", "Safe travels! Mind the dark out there.", "See you around. I'll probably be somewhere else!"
   };

   public ShopScreen(int entityId, int dd, List<ShopScreen.CWare> wares, String greeting, int restockSecs) {
      super(Text.literal("Shop"));
      this.entityId = entityId;
      this.dd = dd;
      this.wares = wares;
      this.greeting = greeting;
      this.restockAtMs = System.currentTimeMillis() + (long)restockSecs * 1000L;
      this.say(greeting);
   }

   static void registerClient() {
      Net121.registerClientReceiver(Shopkeeper.SHOP_OPEN, (client, handler, buf, sender) -> {
         int id = buf.readVarInt();
         int money = buf.readVarInt();
         List raw = readRaw(buf);
         String greet = buf.readString();
         int secs = buf.isReadable() ? buf.readVarInt() : 600;
         client.execute(() -> {
            TurnBattleClientMod.clientDarkDollars = money;
            client.setScreen(new ShopScreen(id, money, decode(client, raw), greet, secs));
         });
      });
      Net121.registerClientReceiver(Shopkeeper.SHOP_RESULT, (client, handler, buf, sender) -> {
         int money = buf.readVarInt();
         List raw = readRaw(buf);
         String msg = buf.readString();
         boolean bought = buf.readBoolean();
         client.execute(() -> {
            TurnBattleClientMod.clientDarkDollars = money;
            if (client.currentScreen instanceof ShopScreen sc) {
               sc.result(money, decode(client, raw), msg, bought);
            }
         });
      });
   }

   private static List<Object[]> readRaw(PacketByteBuf buf) {
      int n = buf.readVarInt();
      List out = new ArrayList<>();

      for (int i = 0; i < n; i++) {
         out.add(new Object[]{buf.readNbt(), buf.readVarInt(), buf.readVarInt()});
      }

      return out;
   }

   private static List<ShopScreen.CWare> decode(MinecraftClient client, List<Object[]> raw) {
      List out = new ArrayList<>();
      if (client.world == null) {
         return out;
      } else {
         for (Object[] r : raw) {
            ItemStack st = r[0] instanceof NbtCompound nbt
               ? ItemStack.fromNbt(client.world.getRegistryManager(), nbt).orElse(ItemStack.EMPTY)
               : ItemStack.EMPTY;
            if (!st.isEmpty()) {
               out.add(new ShopScreen.CWare(st, (Integer)r[1], (Integer)r[2]));
            }
         }

         return out;
      }
   }

   private void result(int money, List<ShopScreen.CWare> newWares, String msg, boolean bought) {
      this.waiting = false;
      this.dd = money;
      this.wares = newWares;
      this.rightMsg = msg;
      this.rightMsgUntil = this.tick + 60;
      if (bought) {
         this.play(TurnBattleSounds.SND_ITEM, 1.0F);
      }

      this.mode = ShopScreen.Mode.BUY;
      this.clampBuy();
   }

   private void buy(int index) {
      this.waiting = true;
      PacketByteBuf b = PacketByteBufs.create();
      b.writeVarInt(this.entityId);
      b.writeVarInt(index);
      Net121.sendToServer(Shopkeeper.SHOP_BUY, b);
   }

   private void say(String line) {
      this.typed = line;
      this.shown = 0.0F;
   }

   private boolean typing() {
      return this.shown < (float)this.typed.length();
   }

   public void tick() {
      this.tick++;
      if (this.leaveAt >= 0 && this.tick >= this.leaveAt) {
         this.close();
      } else {
         if (this.client != null && this.client.player != null && this.tick % 10 == 0) {
            Entity e = this.client.world != null ? this.client.world.getEntityById(this.entityId) : null;
            if (e == null || e.squaredDistanceTo(this.client.player) > 100.0) {
               this.close();
            }
         }
      }
   }

   public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
   }

   public void render(DrawContext ctx, int mx, int my, float delta) {
      this.animate(delta);
      this.s = Math.max(0.5F, Math.min((float)this.width / 320.0F, (float)this.height / 240.0F));
      this.ox = ((float)this.width - 320.0F * this.s) / 2.0F;
      this.oy = ((float)this.height - 240.0F * this.s) / 2.0F;
      MatrixStack m = ctx.getMatrices();
      if (this.panelSlide > 0.01F) {
         ctx.enableScissor(this.rx(0.0F), this.ry(0.0F), this.rx(320.0F), this.ry(118.0F));
         m.push();
         m.translate(this.ox, this.oy, 400.0F);
         m.scale(this.s, this.s, 1.0F);
         this.drawItemPanel(ctx);
         m.pop();
         ctx.disableScissor();
      }

      m.push();
      m.translate(this.ox, this.oy, 600.0F);
      m.scale(this.s, this.s, 1.0F);
      this.drawLeftBox(ctx);
      this.drawRightBox(ctx);
      m.pop();
   }

   private void animate(float delta) {
      long t = System.nanoTime();
      float dt = this.lastFrameNanos == 0L ? 0.0F : Math.min(5.0F, (float)(t - this.lastFrameNanos) / 5.0E7F);
      this.lastFrameNanos = t;
      this.now = (float)this.tick + delta;
      if (this.typing()) {
         int before = (int)this.shown;
         this.shown = Math.min((float)this.typed.length(), this.shown + 1.25F * dt);

         for (int i = before; i < (int)this.shown; i++) {
            if (i % 2 == 0 && !Character.isWhitespace(this.typed.charAt(i))) {
               this.play(TurnBattleSounds.SND_TEXT, 1.0F);
               break;
            }
         }
      }

      float want = this.mode != ShopScreen.Mode.BUY && this.mode != ShopScreen.Mode.CONFIRM ? 0.0F : 1.0F;
      this.panelSlide = this.panelSlide + (want - this.panelSlide) * (1.0F - (float)Math.pow(0.65, (double)dt));
      if (Math.abs(want - this.panelSlide) < 0.003F) {
         this.panelSlide = want;
      }
   }

   private void box(DrawContext ctx, int x0, int y0, int x1, int y1) {
      DarkBox.draw(ctx, x0, y0, x1, y1);
   }

   private void drawLeftBox(DrawContext ctx) {
      int x0 = 4;
      int y0 = 122;
      int x1 = 206;
      int y1 = 236;
      this.box(ctx, x0, y0, x1, y1);
      switch (this.mode) {
         case BUY:
         case CONFIRM:
            this.drawWareList(ctx, x0, y0, x1);
            break;
         case TALK:
            this.drawTopics(ctx, x0, y0);
            break;
         default:
            String t = this.typed.substring(0, Math.min(this.typed.length(), (int)this.shown));
            int y = y0 + 10;
            ctx.drawText(this.textRenderer, Text.literal("*"), x0 + 8, y, -1, false);

            for (OrderedText l : this.textRenderer.wrapLines(Text.literal(t), x1 - x0 - 30)) {
               ctx.drawText(this.textRenderer, l, x0 + 18, y, -1, false);
               y += 12;
            }
      }
   }

   private void drawWareList(DrawContext ctx, int x0, int y0, int x1) {
      int rows = this.wares.size() + 1;

      for (int r = 0; r < 8 && this.buyScroll + r < rows; r++) {
         int i = this.buyScroll + r;
         int y = y0 + 9 + r * 13;
         boolean sel = i == this.buySel;
         if (sel && this.mode == ShopScreen.Mode.BUY || sel && this.mode == ShopScreen.Mode.CONFIRM) {
            this.soul(ctx, x0 + 7, y);
         }

         if (i == this.wares.size()) {
            ctx.drawText(this.textRenderer, "Exit", x0 + 20, y, -1, false);
         } else {
            ShopScreen.CWare w = this.wares.get(i);
            boolean out = w.stock == 0;
            String price = out ? "SOLD OUT" : w.price + " D$";
            int priceW = this.textRenderer.getWidth(price);
            int priceCol = out ? -8355712 : (this.dd >= w.price ? -1 : -5226432);
            String name = w.stack.getName().getString();
            int room = x1 - 10 - priceW - 6 - (x0 + 20);
            if (this.textRenderer.getWidth(name) > room) {
               name = this.textRenderer.trimToWidth(name, room - this.textRenderer.getWidth("..")) + "..";
            }

            ctx.drawText(this.textRenderer, name, x0 + 20, y, out ? -8355712 : -1, false);
            ctx.drawText(this.textRenderer, price, x1 - 10 - priceW, y, priceCol, false);
         }
      }

      if (this.buyScroll > 0) {
         ctx.drawText(this.textRenderer, "^", x1 - 12, y0 + 1, -8355712, false);
      }

      if (this.buyScroll + 8 < rows) {
         ctx.drawText(this.textRenderer, "v", x1 - 12, y0 + 104, -8355712, false);
      }
   }

   private void drawTopics(DrawContext ctx, int x0, int y0) {
      for (int i = 0; i < TOPICS.length; i++) {
         int y = y0 + 10 + i * 13;
         if (i == this.talkSel) {
            this.soul(ctx, x0 + 7, y);
         }

         ctx.drawText(this.textRenderer, TOPICS[i], x0 + 20, y, -1, false);
      }
   }

   private void drawRightBox(DrawContext ctx) {
      int x0 = 210;
      int y0 = 122;
      int x1 = 316;
      int y1 = 236;
      this.box(ctx, x0, y0, x1, y1);
      int tx = x0 + 8;
      int w = x1 - x0 - 16;
      switch (this.mode) {
         case MENU:
         case TALK_REPLY:
         case LEAVING:
            for (int i = 0; i < MENU.length; i++) {
               int y = y0 + 10 + i * 15;
               if (i == this.menuSel && this.mode == ShopScreen.Mode.MENU) {
                  this.soul(ctx, tx, y);
               }

               ctx.drawText(this.textRenderer, MENU[i], tx + 13, y, -1, false);
            }
            break;
         case BUY:
            String m = this.rightMsg != null && this.tick < this.rightMsgUntil ? this.rightMsg : (this.waiting ? "..." : "What would you like to buy?");
            this.wrap(ctx, m, tx, y0 + 10, w, -1);
            break;
         case CONFIRM:
            ShopScreen.CWare cw = this.buySel < this.wares.size() ? this.wares.get(this.buySel) : null;
            this.wrap(ctx, "Buy it for", tx, y0 + 10, w, -1);
            ctx.drawText(this.textRenderer, (cw != null ? cw.price : 0) + " D$ ?", tx, y0 + 22, -256, false);

            for (int i = 0; i < 2; i++) {
               int y = y0 + 44 + i * 15;
               if (i == this.confirmSel) {
                  this.soul(ctx, tx, y);
               }

               ctx.drawText(this.textRenderer, i == 0 ? "Yes" : "No", tx + 13, y, -1, false);
            }
            break;
         case TALK:
            this.wrap(ctx, "Talk about what?", tx, y0 + 10, w, -1);
      }

      ctx.fill(x0 + 4, y1 - 20, x1 - 4, y1 - 19, -12566464);
      ctx.drawText(this.textRenderer, "D$ " + this.dd, tx, y1 - 14, -1, false);
   }

   private void drawItemPanel(DrawContext ctx) {
      int x0 = 214;
      int x1 = 316;
      int h = 110;
      int y0 = Math.round(118.0F - (float)h * this.panelSlide - 4.0F);
      int y1 = y0 + h;
      this.box(ctx, x0, y0, x1, y1);
      if (this.buySel >= this.wares.size()) {
         this.wrap(ctx, "Leave the shop's wares be.", x0 + 6, y0 + 8, x1 - x0 - 12, -8355712);
      } else {
         ShopScreen.CWare w = this.wares.get(this.buySel);
         int y = y0 + 6;
         ctx.drawItem(w.stack, x0 + 6, y);
         int nameW = x1 - x0 - 30;

         for (OrderedText l : this.textRenderer.wrapLines(w.stack.getName().copy().formatted(Formatting.YELLOW), nameW)) {
            ctx.drawText(this.textRenderer, l, x0 + 25, y + 4, -256, false);
            y += 10;
         }

         y = Math.max(y + 2, y0 + 26);
         List tip = battleLines(w.stack);
         MatrixStack m = ctx.getMatrices();
         float k = 0.75F;
         int lineW = (int)((float)(x1 - x0 - 12) / k);
         int bottom = y1 - 16;
         m.push();
         m.translate((float)(x0 + 6), (float)y, 0.0F);
         m.scale(k, k, 1.0F);
         int ly = 0;

         label47:
         for (int i = 0; i < tip.size(); i++) {
            Text t = tip.get(i);
            if (t.getString().isBlank()) {
               ly += 4;
            } else {
               for (OrderedText l : this.textRenderer.wrapLines(t, lineW)) {
                  if ((float)y + (float)(ly + 9) * k > (float)bottom) {
                     ctx.drawText(this.textRenderer, "...", 0, ly, -8355712, false);
                     break label47;
                  }

                  ctx.drawText(this.textRenderer, l, 0, ly, -3618616, false);
                  ly += 10;
               }
            }
         }

         m.pop();
         String stock = w.stock < 0 ? "Plenty in stock" : (w.stock == 0 ? "Sold out" : "Only " + w.stock + " left!");
         ctx.drawText(this.textRenderer, stock, x0 + 6, y1 - 14, w.stock == 0 ? -8355712 : -8323200, false);
      }
   }

   private static List<Text> battleLines(ItemStack st) {
      List out = new ArrayList<>();
      WeaponStats ws = ArmorStats.isArmorItem(st) ? null : WeaponStats.fromStack(st);
      ArmorStats as = ArmorStats.isArmorItem(st) ? ArmorStats.fromStack(st) : null;
      if (ws != null) {
         out.add(Text.literal(ws.isQuirk ? "ODDITY WEAPON" : "Battle Stats").formatted(ws.isQuirk ? Formatting.GOLD : Formatting.YELLOW));
         out.add(Text.literal("+" + ws.baseDamage + (ws.magic ? " Magic DMG" : " DMG")).formatted(ws.magic ? Formatting.LIGHT_PURPLE : Formatting.WHITE));
         out.add(Text.literal("+" + ws.bonusAmt + " " + WeaponStats.STAT_NAMES[ws.bonusStat]).formatted(Formatting.AQUA));
         if (ws.isQuirk) {
            out.add(Text.literal("+10 " + WeaponStats.STAT_NAMES[ws.quirkStat]).formatted(Formatting.GREEN));
            out.add(Text.literal("-10 " + WeaponStats.STAT_NAMES[ws.quirkMinus]).formatted(Formatting.RED));
         }

         for (Affinity elem : ws.getElements()) {
            out.add(Text.literal(elem.displayName + " Element").styled(st2 -> st2.withColor(TextColor.fromRgb(elem.color))));
         }
      } else if (as != null) {
         ArmorStats.Tier tier = ArmorStats.tierOf(st);
         out.add(Text.literal("Battle Stats").formatted(Formatting.YELLOW));
         if (tier != null) {
            out.add(Text.literal(tier.reductionPercent + "% DMG reduction").formatted(Formatting.WHITE));
         }

         out.add(Text.literal("+" + as.bonusAmt + " " + ArmorStats.STAT_NAMES[as.bonusStat]).formatted(Formatting.AQUA));
      } else {
         FoodComponent food = (FoodComponent)st.get(DataComponentTypes.FOOD);
         if (food != null) {
            int hp = (int)(((float)(food.nutrition() * 2) + food.saturation() * 2.0F) * 2.0F);
            out.add(Text.literal("Recovers " + hp + " HP in battle").formatted(Formatting.RED));
         }
      }

      return out;
   }

   private void wrap(DrawContext ctx, String text, int x, int y, int w, int col) {
      for (OrderedText l : this.textRenderer.wrapLines(Text.literal(text), w)) {
         ctx.drawText(this.textRenderer, l, x, y, col, false);
         y += 11;
      }
   }

   private void soul(DrawContext ctx, int x, int y) {
      SoulType soul = TurnBattleClientMod.clientSoul;
      Identifier tex = soul != null ? soul.getTexture() : DEFAULT_SOUL;
      ctx.drawTexture(tex, x, y - 1, 9, 9, 0.0F, 0.0F, 9, 9, 9, 9);
   }

   private int rx(float v) {
      return Math.round(this.ox + v * this.s);
   }

   private int ry(float v) {
      return Math.round(this.oy + v * this.s);
   }

   private double vx(double real) {
      return (real - (double)this.ox) / (double)this.s;
   }

   private double vy(double real) {
      return (real - (double)this.oy) / (double)this.s;
   }

   public boolean keyPressed(int key, int scan, int mods) {
      boolean up = key == 265 || key == 87;
      boolean down = key == 264 || key == 83;
      boolean ok = key == 90 || key == 257 || key == 32 || key == 335;
      boolean back = key == 88 || key == 256 || key == 340 || key == 344;
      if (up) {
         this.move(-1);
         return true;
      } else if (down) {
         this.move(1);
         return true;
      } else if (key != 263 && key != 65) {
         if (key != 262 && key != 68) {
            if (ok) {
               this.confirm();
               return true;
            } else if (back) {
               this.back();
               return true;
            } else {
               return super.keyPressed(key, scan, mods);
            }
         } else {
            if (this.mode == ShopScreen.Mode.BUY) {
               this.move(8);
            }

            return true;
         }
      } else {
         if (this.mode == ShopScreen.Mode.BUY) {
            this.move(-8);
         }

         return true;
      }
   }

   private void move(int d) {
      switch (this.mode) {
         case MENU:
            this.menuSel = Math.floorMod(this.menuSel + d, MENU.length);
            this.play(TurnBattleSounds.MENU_MOVE, 1.0F);
            break;
         case BUY:
            int rows = this.wares.size() + 1;
            int was = this.buySel;
            this.buySel = Math.abs(d) == 1 ? Math.floorMod(this.buySel + d, rows) : Math.max(0, Math.min(rows - 1, this.buySel + d));
            this.clampBuy();
            if (was != this.buySel) {
               this.play(TurnBattleSounds.MENU_MOVE, 1.0F);
            }
            break;
         case CONFIRM:
            this.confirmSel = 1 - this.confirmSel;
            this.play(TurnBattleSounds.MENU_MOVE, 1.0F);
            break;
         case TALK:
            this.talkSel = Math.floorMod(this.talkSel + d, TOPICS.length);
            this.play(TurnBattleSounds.MENU_MOVE, 1.0F);
      }
   }

   private void clampBuy() {
      int rows = this.wares.size() + 1;
      this.buySel = Math.max(0, Math.min(rows - 1, this.buySel));
      if (this.buySel < this.buyScroll) {
         this.buyScroll = this.buySel;
      }

      if (this.buySel >= this.buyScroll + 8) {
         this.buyScroll = this.buySel - 8 + 1;
      }

      this.buyScroll = Math.max(0, Math.min(Math.max(0, rows - 8), this.buyScroll));
   }

   private void confirm() {
      switch (this.mode) {
         case MENU:
            if (this.typing()) {
               this.shown = (float)this.typed.length();
               return;
            }

            this.play(TurnBattleSounds.MENU_SELECT, 1.0F);
            switch (this.menuSel) {
               case 0:
                  this.mode = ShopScreen.Mode.BUY;
                  this.rightMsg = null;
                  this.clampBuy();
                  return;
               case 1:
                  this.say(SELL_LINES[(int)(Math.random() * (double)SELL_LINES.length)]);
                  return;
               case 2:
                  this.mode = ShopScreen.Mode.TALK;
                  this.talkSel = 0;
                  return;
               default:
                  this.leave();
                  return;
            }
         case BUY:
            if (this.waiting) {
               return;
            }

            this.play(TurnBattleSounds.MENU_SELECT, 1.0F);
            if (this.buySel >= this.wares.size()) {
               this.backToMenu("Anything else?");
               return;
            }

            ShopScreen.CWare w = this.wares.get(this.buySel);
            if (w.stock == 0) {
               this.rightMsg = "That one's all gone, sorry!";
               this.rightMsgUntil = this.tick + 50;
               return;
            }

            this.mode = ShopScreen.Mode.CONFIRM;
            this.confirmSel = 0;
            break;
         case CONFIRM:
            this.play(TurnBattleSounds.MENU_SELECT, 1.0F);
            if (this.confirmSel == 0) {
               ShopScreen.CWare w = this.wares.get(this.buySel);
               if (this.dd < w.price) {
                  this.rightMsg = "You don't have enough D$ for that.";
                  this.rightMsgUntil = this.tick + 60;
                  this.mode = ShopScreen.Mode.BUY;
                  return;
               }

               this.mode = ShopScreen.Mode.BUY;
               this.buy(this.buySel);
            } else {
               this.mode = ShopScreen.Mode.BUY;
               this.rightMsg = "Have a think about it.";
               this.rightMsgUntil = this.tick + 40;
            }
            break;
         case TALK:
            this.play(TurnBattleSounds.MENU_SELECT, 1.0F);
            if (this.talkSel == TOPICS.length - 1) {
               this.backToMenu("Anything else?");
               return;
            }

            this.mode = ShopScreen.Mode.TALK_REPLY;
            this.say(this.talk(this.talkSel));
            break;
         case TALK_REPLY:
            if (this.typing()) {
               this.shown = (float)this.typed.length();
               return;
            }

            this.mode = ShopScreen.Mode.TALK;
            break;
         case LEAVING:
            this.close();
      }
   }

   private void back() {
      switch (this.mode) {
         case MENU:
            if (this.typing()) {
               this.shown = (float)this.typed.length();
            } else {
               this.leave();
            }
            break;
         case BUY:
            this.backToMenu("Anything else?");
            break;
         case CONFIRM:
            this.mode = ShopScreen.Mode.BUY;
            break;
         case TALK:
            this.backToMenu("Anything else?");
            break;
         case TALK_REPLY:
            if (this.typing()) {
               this.shown = (float)this.typed.length();
            } else {
               this.mode = ShopScreen.Mode.TALK;
            }
            break;
         case LEAVING:
            this.close();
      }
   }

   private void backToMenu(String line) {
      this.mode = ShopScreen.Mode.MENU;
      this.say(line);
   }

   private void leave() {
      this.mode = ShopScreen.Mode.LEAVING;
      this.say(BYE_LINES[(int)(Math.random() * (double)BYE_LINES.length)]);
      this.leaveAt = this.tick + 40;
   }

   public boolean mouseScrolled(double mx, double my, double h, double v) {
      if (v != 0.0) {
         this.move(v > 0.0 ? -1 : 1);
      }

      return true;
   }

   public void mouseMoved(double mx, double my) {
      double x = this.vx(mx);
      double y = this.vy(my);
      switch (this.mode) {
         case MENU:
            if (x < 210.0 || x > 316.0) {
               return;
            }

            int i = (int)Math.floor((y - 122.0 - 8.0) / 15.0);
            if (i >= 0 && i < MENU.length && i != this.menuSel) {
               this.menuSel = i;
               this.play(TurnBattleSounds.MENU_MOVE, 1.0F);
            }
            break;
         case BUY:
            if (x < 4.0 || x > 206.0) {
               return;
            }

            int r = (int)Math.floor((y - 122.0 - 7.0) / 13.0);
            int was = this.buySel;
            if (r >= 0 && r < 8 && this.buyScroll + r <= this.wares.size()) {
               this.buySel = this.buyScroll + r;
            }

            if (was != this.buySel) {
               this.play(TurnBattleSounds.MENU_MOVE, 1.0F);
            }
            break;
         case CONFIRM:
            if (x < 210.0 || x > 316.0) {
               return;
            }

            int i = (int)Math.floor((y - 122.0 - 42.0) / 15.0);
            if ((i == 0 || i == 1) && i != this.confirmSel) {
               this.confirmSel = i;
               this.play(TurnBattleSounds.MENU_MOVE, 1.0F);
            }
            break;
         case TALK:
            if (x < 4.0 || x > 206.0) {
               return;
            }

            int rx = (int)Math.floor((y - 122.0 - 8.0) / 13.0);
            if (rx >= 0 && rx < TOPICS.length && rx != this.talkSel) {
               this.talkSel = rx;
               this.play(TurnBattleSounds.MENU_MOVE, 1.0F);
            }
      }
   }

   public boolean mouseClicked(double mx, double my, int button) {
      if (button == 1) {
         this.back();
         return true;
      } else if (button != 0) {
         return super.mouseClicked(mx, my, button);
      } else {
         double x = this.vx(mx);
         double y = this.vy(my);
         boolean inLeft = x >= 4.0 && x <= 206.0 && y >= 122.0 && y <= 236.0;
         boolean inRight = x >= 210.0 && x <= 316.0 && y >= 122.0 && y <= 236.0;

         boolean act = switch (this.mode) {
            case MENU -> this.typing() || inRight;
            case BUY, TALK -> inLeft;
            case CONFIRM -> inRight;
            default -> true;
         };
         if (act) {
            this.confirm();
         }

         return true;
      }
   }

   public boolean shouldPause() {
      return false;
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   private void play(SoundEvent ev, float pitch) {
      if (this.client != null) {
         this.client.getSoundManager().play(PositionedSoundInstance.master(ev, pitch));
      }
   }

   private String talk(int topic) {
      return switch (topic) {
         case 0 -> "Me? Just a humble Darkner with a cart full of wares. I set up wherever the customers are. Today, that's here!";
         case 1 -> "Every Dark World's a little different, but they've all got monsters, and monsters all drop D$. Good for business!";
         case 2 -> "Dark Dollars! Beat the monsters here and they'll drop some. The stronger the monster, the more you get. Finish a fight with a full TP bar and you'll get up to twice as much!";
         case 3 -> {
            long left = Math.max(0L, (this.restockAtMs - System.currentTimeMillis()) / 1000L);
            yield "I get new stock every ten minutes. The next lot comes in "
               + left / 60L
               + ":"
               + String.format("%02d", left % 60L)
               + ". Pearls, rods and skulls, I've always got.";
         }
         default -> "...";
      };
   }

   static final class CWare {
      final ItemStack stack;
      final int price;
      final int stock;

      CWare(ItemStack stack, int price, int stock) {
         this.stack = stack;
         this.price = price;
         this.stock = stock;
      }
   }

   private static enum Mode {
      MENU,
      BUY,
      CONFIRM,
      TALK,
      TALK_REPLY,
      LEAVING;
   }
}
