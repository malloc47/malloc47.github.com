---
layout: post
title: vim -> emacs
permalink: /blog/vim-to-emacs/
---

After having grown up in the terminal with vim at my side, I've been transitioning to emacs. There's no shortage of guides for making the leap, but there's a number of features in vim that operate very differently in emacs, or do not map cleanly to a single feature. There's nothing like not knowing how to get that *one* feature you rely on when making the vim-&gt;emacs leap to make you want to run back to familiar territory. Here's a few of the discoveries I've made along the way.

    dd -> C-Shift-Backspace

**C-k** is more often cited as the analog for **dd** (and it is indeed more flexible in many situations), but as far as raw functionality is concerned, sometimes you just want the current line to go away, regardless of where the cursor is. Unfortunately, the** C-Shift-Backspace** keybinding may not be triggered when you're running emacs in a screen or xterm (emacs -nw), but for the majority of standard emacs use, it's a feature-compatible substitute.

    % -> C-M-n, C-M-p

Traversing pairs of grouping characters (e.g. (), \[\], {}, etc.) with vim's % key is invaluable, but emacs not only replicates this feature, but adds a few new bits of functionality too. In general **C-n** and **C-p** will traverse to the next or previous line, respectively, but **C-M-n** and **C-M-p** will traverse forward or backward over the current *list*. Since emacs has a major mode related to most languages you could find yourself working in (and if not, it's not difficult to find or make one) that will do a rudimentary parse of the code into proper tokens, **C-M-n** and **C-M-p** can intelligently jump to the beginning or end of parenthetical structures, words, or blocks with equal effectiveness. As a bonus, you also get **C-M-d** (if your window manager hasn't stolen that keybinding) and **C-M-u** that go down or up (respectively) in the current structure---i.e., **C-M-d** will place the cursor inside the parenthetical statement, ready to iterate over the items inside the parentheses with **C-M-n**, while **C-M-u** will place the cursor at the beginning of the parenthesis or block structure enclosing the cursor. Check the major mode you are in for even more context-sensitive commands (e.g., **M-a** and **M-e** to go the beginning or ending of a statement in the C++ major mode). Another instance where it *seems* like emacs adds overhead to the preciously minimalistic vim keybindings, but having two keys instead of one adds immeasurable navigational flexibility.

    f, F, t, T -> C-s, C-r

This may not be immediately intuitive, but the best mapping for vim's nifty "forward (or backward) to character" feature is emacs's standard search function. While, yes, the concession is that it requires three keystrokes (you must press enter after searching in emacs to position the cursor) instead of one, you get the same functionality, with the added benefit that you can search more than one character easily, and you don't have to retype the character you want to jump to, as you would in vim.

    [n]gg, :[n] -> M-g g

Though emacs integrates so well into most REPLs that you don't often need to jump to specific line numbers manually, it's still trivial to hop to specific line numbers with ease. Again, you're sacrificing more keystrokes but, as with everything in emacs, you can remap anything you use often. Alternately, consider using the faster **C-\[n\] M-g g** variant to prepend the line number instead of having to specify it (and hit enter) in the mini-buffer.

    "[register]p -> C-y M-y

The kill ring in emacs isn't remotely complex, so it's hardly worth mentioning except that it varies greatly from vim's model of using registers to store yanked text. Instead of having to specify a register, as you do in vim, you simply paste whatever text happens to be in the ring into your document/code with **C-y**, then loop through the various yanks with **M-y**. Coming from a vim world, where every yank requires a register that contains only one snippet of text, the value of having yanks (or kills) you can scroll through (in the exact context you wish to paste them) is quite clear (and can actually result in fewer keystrokes for complicated yanks). As a bonus, only having to use **C-w** or **M-w** to yank text requires fewer keystrokes than having to use **"\[reg\]y** in vim too.

    q[register] ... q -> C-x ( ... C-x ) M-x name-last-kbd-macro [name]

The only surface difference between vim and emacs for keyboard macros is that vim assigns keyboard macros to a register by default, but emacs requires **M-x name-last-kbd-macro** to cache more than one macro at a time. The difference becomes more stark once you explore multiple macros, as you can apply a named keyboard macro by using **M-x \[name\]**, since emacs saves the keyboard macro as a standard emacs command. As a bonus you can use the **M-x insert-kbd-macro** to save the macro to your *.emacs* file for future use.

    u, C-r -> C-/

This is another emacs feature that's very simple to understand, but where vim logic may trip you up. Most vim-&gt;emacs guides will clue you in that vim's **u** maps to emacs **C-/** (**C-_** or **C-x u**, but those are more of a hassle), which is indeed correct. But the underlying "redo" logic is a bit different. Instead of vim's separate **C-r** redo command, emacs lets you "undo your undos" (again, think yank ring). So after a series of consecutive **C-/**s, all you need do is interrupt the sequence with a command that does not produce any undo history (I typically use a movement command like **C-f**), and then use **C-/** again, which will then, essentially, have become a "redo" command. This will become second nature quickly, but it's a very different model from the inferior method used by vim (and a large number of other editors).

    : -> M-:, M-x

Only thing worth mentioning here is the difference between executing direct elisp code (**M-:**) and emacs commands (**M-x**), which is not a distinction that vim has to make.

    :tabn, gf -> C-x b

Vim actually does allow for "hidden" buffers, so it can operate in a very emacsish way, if desired, but it a rarely-used feature; one that emacs adopts by default. Emacs doesn't have "tabs" to switch between, per se, but the way it handles buffers is vastly more powerful than any tab or hidden buffer in vim, minus the "tab" aesthetic (and ido-mode or **C-x C-b** is more than capable of providing a list if that is what is needed).

    Screen Line Movement -> Logical Line Movement

Emacs's **C-n** and **C-p** move over screen lines (the line breaks you see on the screen) by default, while vim's **j**,**k** moves over logical lines (line breaks that are actually in the file). While vim's logical line movement can cause problems with long lines (you have to use **gj** and **gk** to switch over to screen lines), you can switch emacs to logical line movement, if you prefer it, with


    (setp line-move-visual nil)

which can be easily entered with **M-:** in emacs to try it out, or added to your `.emacs` file to save the setting.
