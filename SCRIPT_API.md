# SushiTriggers script API

Scripts are ES5 JavaScript (Rhino): `var`/`function` only, no arrow functions, no `let/const`, no `for...of`, no template strings. Use `Packages.<class>` for Java classes.
Every API below is safe: it returns an empty value instead of throwing when something is missing.

## Triggers: `register("name", function (...) {})`
| name | arguments |
|---|---|
| tick | - |
| chat | message (plain text, no colors), event |
| event | category, text (every line of the Events log: damage, knockback, clicks, keys, blocks, chat...) |
| containerOpen / containerClose | name, slotCount |
| renderOverlay | - (draw with Hud.text/rect/outline) |
| packetReceived | packet, event (`cancel(event)` blocks it) |
| gameLoad / gameUnload / worldLoad ... | - |
| command | `.setName("name")`, receives the typed arguments |

Unknown trigger names never throw. They are accepted and simply never fire.

## Globals
- `ChatLib.chat("&bText")` local message, `print(x)` to the terminal, `FileLib`, `MathLib`
- `Client`: `chat(msg)`, `say(msg)` (send chat), `command("warp hub")`, `getFps()`, `getServerAddress()`, `getScreenName()`, `getScreenTitle()`, `closeScreen()`, `copy(text)`, `clickSlot(slot, button, "PICKUP"|"QUICK_MOVE"|"SWAP"|"CLONE"|"THROW")`
- `Player`: `getName() getX() getY() getZ() getYaw() getPitch() getHealth() getMaxHealth() getAbsorption() getFood() getXpLevel() isSneaking() isSprinting() isOnGround() isOnFire() isInWater() getMotion() setMotion(x,y,z) getHeldItem() getOffhandItem() getSelectedSlot() setSelectedSlot(i) getInventory() getItem(slot) findItem(text) getDimension() getEffects()`
- `World`: `getBlockAt(x,y,z) getDayTime() getGameTime() isRaining() getEntities(radius) getPlayerNames() getScoreboard()`
- `Container`: `isOpen() getTitle() getSize() getItem(slot) getItems() getCursorItem() click(slot, button, mode) close()`
- Item: `.name .count .id .slot .lore (array of strings) .empty .hasLore("text")`
- Entity: `.name .type .x .y .z .distance`
- `Hud`: `panel(id, title, 0xRRGGBB, linesArray, colorsArray, x, y)`, `set(id, text, x, y)`, `remove(id)`, `width()`, `height()`, `textWidth(text)`; inside renderOverlay: `text(str,x,y,color)`, `rect(x,y,w,h,0xAARRGGBB)`, `outline(...)`
