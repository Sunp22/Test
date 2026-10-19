package com.pixelcite.turnbattle;

import java.util.List;

final class DepthsLore {
   static final String READ = "(... ?&There's nothing written there.)";
   static final List<DepthsLore.Lore> ALL = List.of(
      new DepthsLore.Lore(
         "(\"Hey! Did you know that weapons can be rolled with elements?\")%(\"They can increase your existing spells of that element. This is called a 'STAB.'\")%(\"The more you know!\")",
         DepthsLore.Effect.NONE
      ),
      new DepthsLore.Lore("(\"Try sleeping in a bed at Level 50! Something interesting happens!\")", DepthsLore.Effect.NONE),
      new DepthsLore.Lore("(\"Do you remember what the city looked like?\")", DepthsLore.Effect.NONE),
      new DepthsLore.Lore("Psst. Hey. I'm a speaking sign. Before they find out about me, I want you to know, that-", DepthsLore.Effect.NONE),
      new DepthsLore.Lore("(It's nothing but garbage words.)", DepthsLore.Effect.NONE),
      new DepthsLore.Lore("(\"Who wrote all of these?\")", DepthsLore.Effect.NONE),
      new DepthsLore.Lore("(\"The skyline was so pretty.\")", DepthsLore.Effect.NONE),
      new DepthsLore.Lore("(\"She held her breath for the last time.\")%(\"Seems to be a trend around here.\")", DepthsLore.Effect.NONE),
      new DepthsLore.Lore("(\"The Device had been modified to work with different software.\")", DepthsLore.Effect.NONE),
      new DepthsLore.Lore(
         "(\"He wasn't able to see the original story through, but maybe the scientist can find new happiness through here.\")", DepthsLore.Effect.NONE
      ),
      new DepthsLore.Lore(
         "(\"Sometimes too much power is a bad thing.\")%(\"That little alien would know. He didn't stop screaming for ages!\")", DepthsLore.Effect.NONE
      ),
      new DepthsLore.Lore(
         "(\"I wonder how many more heroes will show up to fail.\")%(\"The three prophesized heroes. The eight prophesized heroes.\")%(\"Guess you can't rely on anybody but yourself.\")",
         DepthsLore.Effect.NONE
      ),
      new DepthsLore.Lore(
         "(... The handwriting here is... a lot more, frantic than the others.)%(\"I thought I could change it.\")%(\"I thought, if I was... If we were just...\")%(\"... i'm sorry.\")",
         DepthsLore.Effect.NONE
      ),
      new DepthsLore.Lore(
         "(\"They say the Prophecy only works if it's followed the exact way it's written.\")%(\"... Of course. That's just a rumor.\")%(\"A Theory, if you will. A DEVICE Theory.\")%(\"Penumbra Phantasm The World Revolving Dess Is Missing FRIEND Rudy Knight Papyrus\")",
         DepthsLore.Effect.NONE
      ),
      new DepthsLore.Lore(
         "(\"Fantasy brought into reality, the eldritch creature tried to attach itself to a long discarded vessel.\")%(\"Through doing this, it remained alive.\")",
         DepthsLore.Effect.NONE
      ),
      new DepthsLore.Lore(
         "(\"She went to college to get more knowledge.\")%(\"He went to hell, with his last goodbye he'll never tell.\")", DepthsLore.Effect.NONE
      ),
      new DepthsLore.Lore(
         "(\"They forgot to write the part about the Knight being thwarted by the Chariot of Dust.\")%(\"Don't drink and drive.\")", DepthsLore.Effect.NONE
      ),
      new DepthsLore.Lore("(\"The Cage did not account for their SOUL giving up. The world didn't account for it either.\")", DepthsLore.Effect.NONE),
      new DepthsLore.Lore("(\"This... is an Enderpearl!\")", DepthsLore.Effect.NONE),
      new DepthsLore.Lore(
         "(The sign has what appears to be a whole love story between two dog monsters.)%(You remembered something funny.)%(... What? You didn't do that?)",
         DepthsLore.Effect.NONE
      ),
      new DepthsLore.Lore(
         "(\"How are you even able to open fountains? The sun hasn't even risen yet!\")%(\"Probably won't ever rise again.\")", DepthsLore.Effect.NONE
      ),
      new DepthsLore.Lore("(\"Burning Pressure. You'll meet her soon, if you haven't already.\")", DepthsLore.Effect.NONE),
      new DepthsLore.Lore("(\"It snew.\")", DepthsLore.Effect.NONE),
      new DepthsLore.Lore("(\"You should probably call your parents. How else are you meant to save?!\")", DepthsLore.Effect.NONE),
      new DepthsLore.Lore(
         "(\"Do you think she's still here? I wonder if she experiences the passage of time from the story or from reality.\")", DepthsLore.Effect.NONE
      ),
      new DepthsLore.Lore("(\"You feel your hunger pains crawling on your back.\")%(\"You should probably have a snack!\")", DepthsLore.Effect.NONE),
      new DepthsLore.Lore("(\"Posture check.\")", DepthsLore.Effect.NONE),
      new DepthsLore.Lore("(\"Remember to blink!\")", DepthsLore.Effect.NONE),
      new DepthsLore.Lore("(\"This DEVICE is addictive. Don't forget to take breaks!\")", DepthsLore.Effect.NONE),
      new DepthsLore.Lore(
         "(\"Sometimes, you'd sit alone, atop one of the many buildings.\")%(\"... And like magic, she would always find you when you're at your worst.\")%(\"I mean, it could be magic. You've seen weirder things.\")%(\"... At the very least, you know that she'll always be there for you.\")%(\"... Who is this talking about again? Do you know?\")",
         DepthsLore.Effect.NONE
      ),
      new DepthsLore.Lore(
         "(\"I wonder how many of these signs are talking about the \"you\" in the lantern, or the \"you\" that's carrying it.\")", DepthsLore.Effect.NONE
      ),
      new DepthsLore.Lore("(\"You should try Penumbra Phantasm sometime. The minecraft mod, not the brainrot.\")", DepthsLore.Effect.NONE),
      new DepthsLore.Lore("(\"Pixel, TAKE A BREAK.\")", DepthsLore.Effect.NONE),
      new DepthsLore.Lore(
         "(\"Compared to the last vessel, THIS one seems much more obedient.\")%(\"I don't mean the rabbit, I mean, she's probably not terrible, but...\")%(\"Oh, the other one? Don't you remember? You two fought a lot.\")%(\"Unfortunately, their courage was far too faint to push forward.\")",
         DepthsLore.Effect.NONE
      ),
      new DepthsLore.Lore("(\"The Cage is non-binary. If this bothers you, please uninstall ASAP.\")", DepthsLore.Effect.NONE),
      new DepthsLore.Lore("(\"Finally, in finality, isn't true.\")", DepthsLore.Effect.NONE),
      new DepthsLore.Lore(
         "(\"Imagine being so lovely to be around, that you taught someone to love. To feel. To empathize.\")%(\"Someone who was born without the ability to feel anything.\")%(\"If only you remembered.\")%(... You get the urge to pummel the sign to pieces. You don't know why. It makes you angrier.)",
         DepthsLore.Effect.NONE
      ),
      new DepthsLore.Lore(
         "(\"The Cultists being brought over alongside you is SO funny. They don't even know what they are worshipping!\")%(\"She probably remembers, though. Hard to say.\")%(\"The girl from the second story.\")",
         DepthsLore.Effect.NONE
      ),
      new DepthsLore.Lore("(\"These are far easier to write with 16:9 scaling in mind.\")%(\"Hate writing UTDR textboxes in 4:9\")", DepthsLore.Effect.NONE),
      new DepthsLore.Lore("(\"They'd let the world burn for a chance to see her again.\")", DepthsLore.Effect.NONE),
      new DepthsLore.Lore("(\"Planting a tree where trauma will grow.\")", DepthsLore.Effect.NONE),
      new DepthsLore.Lore(
         "(Well, there is a sign here.)%(A hand reaches out from behind the sign. It causes you to step back in surprise and fear.)%(The hand looks shriveled and old, but still he offers you something.)%(Instinctively, as if waiting your whole life for this, you reach your hand out...)%(You obtained an Egg.)%(Well, there was a hand there.)",
         DepthsLore.Effect.EGG
      ),
      new DepthsLore.Lore(
         "(\"Frankly, I think the \"coldwater\" that the man mentions in Magicant is just bad writing. Buzzword buzzword buzzword!!!\")%(\"I've never actually heard this opinion before, but with the attention the Device Extension is getting, it's only a matter of time.\")",
         DepthsLore.Effect.NONE
      ),
      new DepthsLore.Lore(
         "(\"The Scientist took you at your worst. He allowed you to show your story to the world.\")%(You should be grateful.)", DepthsLore.Effect.NONE
      ),
      new DepthsLore.Lore("(\"It's so dark. You can see right in front of you.\")", DepthsLore.Effect.NONE),
      new DepthsLore.Lore(
         "(The sign is readable for just a moment, before turning and facing it's back to you.)%(It's shy. You certainly can relate.)", DepthsLore.Effect.NONE
      ),
      new DepthsLore.Lore(
         "(\"Whenever you stuttered, that one nerd you knew would always mimic it to make fun of you.\")%(\"It always made you upset. But you'd never voice your feelings.\")%(\"... Even now, you seem angrier at me for insulting him.\")%(\"That's self destructive, Ori. You need to stand up for yourself more.\")",
         DepthsLore.Effect.NONE
      ),
      new DepthsLore.Lore(
         "(The sign has nothing written on it.)%(You take the time to ponder about the person who wrote them.)%(... Are they sentient? Surely, not...&But, it does almost sound like it's talking directly to you in the moment.)",
         DepthsLore.Effect.NONE
      ),
      new DepthsLore.Lore(
         "(This sign only has a picture of a person smiling. It's kind of unsettling.)%(Flowers bloom inside your heart.)", DepthsLore.Effect.NONE
      ),
      new DepthsLore.Lore(
         "(\"That girl... She fell to her knees when The Cage stopped moving. She held them close, begging them to say something. Anything.\")%(\"... She didn't even notice the attack coming straight towards her.\")%(\"The Prince took the attack, and left her alone to fight the monstrosity alone.\")%(\"Somehow, she won. Maybe it was anger. Grief. Or maybe Determination.\")%(\"... Ultimately, she would still fail to stop the Knight. Her immense strength would leave her as she grieved.\")",
         DepthsLore.Effect.NONE
      ),
      new DepthsLore.Lore("I'm a talking sign. If you see my relative, tell them I said hi. They've been looking for you.", DepthsLore.Effect.NONE),
      new DepthsLore.Lore(
         "(\"Is the mention of that one girl you knew getting to you?\")%(\"Does it anger you?\")%(\"... Do you even know why it angers you?\")%(\"Do you remember what she looked like?\")%(\"Do you remember her name?\")%(\"Let me help you. It starts with a-\")",
         DepthsLore.Effect.VANISH
      ),
      new DepthsLore.Lore("(\"I SWEAR, if ONE person asks for GT content...\")", DepthsLore.Effect.NONE),
      new DepthsLore.Lore("(\"Shoutout to Project Soul Skirmish.\")", DepthsLore.Effect.NONE),
      new DepthsLore.Lore("(\"Shoutout to Morningstar.\")", DepthsLore.Effect.NONE),
      new DepthsLore.Lore("(\"Shoutout to Deltatraveler.\")%(\"Or, INTERDREAM now.\")", DepthsLore.Effect.NONE),
      new DepthsLore.Lore(
         "(\"Out of all the Holidays, him being the only one who makes it through the end is sorta, kinda funny. I'm sorry, is that dark?\")",
         DepthsLore.Effect.NONE
      ),
      new DepthsLore.Lore(
         "(\"Uuu!! I'm here too!!! In this creepy water place!!\")%(\"I found a pirate ship!!!... Hey, humans are usually pirates, right?!\")",
         DepthsLore.Effect.NONE
      ),
      new DepthsLore.Lore(
         "(\"I see something in the distance.\")%(\"... I think. But no matter how close I get, I can't reach it.\")%(\"It's always so far away. I don't know if I can even move anymore.\")%(\"But ever since it showed up... It's almost like it's taunting me.\")%(\"... Hey, is there something moving over there?\")",
         DepthsLore.Effect.NONE
      ),
      new DepthsLore.Lore(
         "(\"Try the Sustingus seed!\")%(\"It doesn't have any special functionality, but imagine how funny it would be if it were a cool seed.\")",
         DepthsLore.Effect.NONE
      ),
      new DepthsLore.Lore(
         "(\"You always end up in cages. How funny is that?\")%(\"But this time, you can temporarily seize control of the one carrying your prison. Thanks to him. You'd never survive here on your own.\")%(\"If she knew her SOUL was being controlled by you, I wonder how she would react.\")",
         DepthsLore.Effect.NONE
      ),
      new DepthsLore.Lore(
         "(\"The Prophecy of the DELTA RUNE. It has seen so many different variants.\")%(\"Even here, too. In this time. In this world.\")",
         DepthsLore.Effect.NONE
      ),
      new DepthsLore.Lore("(\"What if I just, randomly gave you a free card draw. Right now.\")", DepthsLore.Effect.CARD),
      new DepthsLore.Lore("(\"I'm the cool sign. I give out free weapons.\")", DepthsLore.Effect.WEAPON),
      new DepthsLore.Lore("(\"I'm the cool sign. I give out free food.\")", DepthsLore.Effect.FOOD),
      new DepthsLore.Lore("(\"I'm the cool sign. I give out free armor.\")", DepthsLore.Effect.ARMOR),
      new DepthsLore.Lore("(\"As of 5, the truth behind the DEVICE will make itself known.\")", DepthsLore.Effect.NONE),
      new DepthsLore.Lore("(\"Maybe these signs have several different authors.\")%(\"You never know. You never even know.\")", DepthsLore.Effect.NONE),
      new DepthsLore.Lore("(\"They put estrogen in my freaking halloween candy. What the heck.\")", DepthsLore.Effect.NONE),
      new DepthsLore.Lore("(\"Trans rights are human rights!\")", DepthsLore.Effect.NONE),
      new DepthsLore.Lore(
         "(\"Get ready for a FIRE joke. Ok so only read the next part after u say something out loud ok ok\")%(\"Say 'knock knock'\")%Knock knock.%(\"Whose there? FREAKING NOBODY! LOLLLLLLLLL\")%(That's not how knock knock jokes go. You are annoyed.)",
         DepthsLore.Effect.NONE
      ),
      new DepthsLore.Lore(
         "(You stop to wonder if these signs have ever been maintained at all.)%(You are so lost in thought, you even forget to read it.)%(You clean a bit of dust off the sign. You hope it appreciates it.)",
         DepthsLore.Effect.NONE
      ),
      new DepthsLore.Lore(
         "(\"Okay so imagine this. Like A THOUSAND people are watching you, on a big stage. And you say a really crappy joke.\")%(\"... And they all start laughing AT YOU, not WITH YOU.\")%(You are viscerally uncomfortable.)",
         DepthsLore.Effect.NONE
      ),
      new DepthsLore.Lore("(\"Make an offense build with a Paula skin and call the vessel 'Maula.' It will be SO funny.\")", DepthsLore.Effect.NONE),
      new DepthsLore.Lore("(\"Does anybody actually read these?...\")", DepthsLore.Effect.NONE),
      new DepthsLore.Lore("(\"If only you had a puppet to purify the Crystals. You'd be able to get one twisted weapon.\")", DepthsLore.Effect.NONE),
      new DepthsLore.Lore(
         "(UNFORTUNATELY, ONE DAY, YOUR LIFE WILL COME TO AN END.)%(... AND WITH IT, YOUR WONDERFUL IDEAS.)%(YOU OWE IT TO YOURSELF, YOU OWE IT TO THE WONDERFUL CREATIONS YOU HAVE MADE.)%(RECORD THESE IDEAS SOMEWHERE. SOMEWHERE THEY CAN LIVE ON.)%(EVERY AND ALL STORY IS PRECIOUS.)",
         DepthsLore.Effect.NONE
      ),
      new DepthsLore.Lore(
         "(\"Dude, be careful about reading signs, they might say some crap like 'You are now breathing manually.'\")%(\"Or, 'you are now blinking manually.'\")%(\"Or, 'You can now feel your tongue in your mouth, and you can't find a comfortable resting spot for it.'\")%(\"Wouldn't that be terrible? If a sign said those things?\")",
         DepthsLore.Effect.NONE
      ),
      new DepthsLore.Lore("(\"Antonio The Ant will be missed.\")", DepthsLore.Effect.NONE),
      new DepthsLore.Lore(
         "(\"Did you know the BASH attacks used in Magicant are really old patterns that were designed for a Kris boss?\")%(\"The old version of the story had you meet Kris, Susie and Ralsei, specifically in Dark Worlds created here.\")",
         DepthsLore.Effect.NONE
      ),
      new DepthsLore.Lore("(\"Data Driven update is gonna be so peak.\")", DepthsLore.Effect.NONE),
      new DepthsLore.Lore("(\"Please pay 9999G to read the contents of this sign.\")%(\"They call that the Uubeeshoft method.\")", DepthsLore.Effect.NONE)
   );

   private DepthsLore() {
   }

   static enum Effect {
      NONE,
      EGG,
      CARD,
      WEAPON,
      FOOD,
      ARMOR,
      VANISH;
   }

   static record Lore(String text, DepthsLore.Effect effect) {
      int key() {
         return this.text.hashCode();
      }
   }
}
