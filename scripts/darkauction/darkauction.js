var Minecraft = Packages.net.minecraft.client.Minecraft;
var Component = Packages.net.minecraft.network.chat.Component;
var UUID = Packages.java.util.UUID;

var enabled = true;
var tickCount = 0;
var S = "§";
var bossId = UUID.randomUUID();
var bossEvent = null;
var failed = false;

var CF = Packages.net.minecraft.ChatFormatting;

function pad(n) {
    return n < 10 ? "0" + n : "" + n;
}

function part(text, color, bold) {
    var c = Component.literal(text);
    c = c.withStyle(color);
    if (bold) c = c.withStyle(CF.BOLD);
    return c;
}

function getInfo() {
    var now = new Date();
    var secondsOfHour = now.getUTCMinutes() * 60 + now.getUTCSeconds();
    var start = 55 * 60;
    var comp = Component.literal("");
    var plain;
    var progress;

    if (secondsOfHour >= start) {
        var left = 3600 - secondsOfHour;
        var t = pad(Math.floor(left / 60)) + ":" + pad(left % 60);
        comp.append(part("Dark Auction ", CF.LIGHT_PURPLE, true));
        comp.append(part("ACTIVE ", CF.LIGHT_PURPLE, true));
        comp.append(part(t, CF.LIGHT_PURPLE, true));
        plain = "Dark Auction ACTIVE " + t;
        progress = left / 300;
    } else {
        var wait = start - secondsOfHour;
        var t2 = pad(Math.floor(wait / 60)) + ":" + pad(wait % 60);
        var color = CF.LIGHT_PURPLE;
        comp.append(part("Dark Auction ", CF.LIGHT_PURPLE, true));
        comp.append(part("in ", CF.LIGHT_PURPLE, false));
        comp.append(part(t2, color, true));
        plain = "Dark Auction in " + t2;
        progress = wait / 3300;
    }

    return { component: comp, text: plain, progress: progress };
}

function fail(msg) {
    if (failed) return;
    failed = true;
    ChatLib.chat("&c[DarkAuction] " + msg);
    print("[DarkAuction] " + msg);
}

var debugLines = [];

function typeName(t) {
    return String(t.getSimpleName());
}

function isBossType(t) {
    return typeName(t).toLowerCase().indexOf("boss") >= 0 && typeName(t).indexOf("Event") < 0 && typeName(t).indexOf("Lerping") < 0;
}

function scanObject(obj, label) {
    var cls = obj.getClass();
    while (cls != null && String(cls.getName()) !== "java.lang.Object") {
        var fields = cls.getDeclaredFields();
        for (var i = 0; i < fields.length; i++) {
            var f = fields[i];
            debugLines.push(label + " field " + cls.getSimpleName() + "." + f.getName() + " : " + typeName(f.getType()));
            if (isBossType(f.getType())) {
                f.setAccessible(true);
                var v = f.get(obj);
                if (v != null) return v;
            }
        }
        var methods = cls.getDeclaredMethods();
        for (var j = 0; j < methods.length; j++) {
            var m = methods[j];
            if (m.getParameterCount() === 0) {
                debugLines.push(label + " method " + cls.getSimpleName() + "." + m.getName() + "() : " + typeName(m.getReturnType()));
                if (isBossType(m.getReturnType())) {
                    m.setAccessible(true);
                    var r = m.invoke(obj);
                    if (r != null) return r;
                }
            }
        }
        cls = cls.getSuperclass();
    }
    return null;
}

function writeDebug() {
    try {
        var w = new java.io.FileWriter("./config/SushiTriggers/bossbar-debug.txt");
        w.write(debugLines.join("\n"));
        w.close();
    } catch (e) {}
}

function getField(obj, name) {
    var cls = obj.getClass();
    while (cls != null) {
        try {
            var f = cls.getDeclaredField(name);
            f.setAccessible(true);
            return f.get(obj);
        } catch (e) {}
        cls = cls.getSuperclass();
    }
    return null;
}

function findOverlay(gui) {
    debugLines = [];
    var found = scanObject(gui, "gui");
    if (found == null) {
        var hud = getField(gui, "hud");
        if (hud != null) found = scanObject(hud, "hud");
    }
    if (found == null) writeDebug();
    return found;
}

function findMap(overlay) {
    var fields = overlay.getClass().getDeclaredFields();
    for (var i = 0; i < fields.length; i++) {
        var f = fields[i];
        if (java.lang.Class.forName("java.util.Map").isAssignableFrom(f.getType())) {
            f.setAccessible(true);
            return f.get(overlay);
        }
    }
    return null;
}

function pickEnum(type, wanted) {
    var constants = type.getEnumConstants();
    for (var i = 0; i < constants.length; i++) {
        if (String(constants[i].name()) === wanted) return constants[i];
    }
    return constants[0];
}

function createEvent(comp, progress) {
    var cls = null;
    var names = ["net.minecraft.client.gui.components.LerpingBossEvent", "net.minecraft.client.gui.components.LerpingBossBar"];
    for (var n = 0; n < names.length && cls == null; n++) {
        try { cls = java.lang.Class.forName(names[n]); } catch (e) {}
    }
    if (cls == null) throw "LerpingBossEvent class not found";

    var ctors = cls.getConstructors();
    for (var i = 0; i < ctors.length; i++) {
        var types = ctors[i].getParameterTypes();
        var args = java.lang.reflect.Array.newInstance(java.lang.Object, types.length);
        var ok = true;
        var enumIndex = 0;
        for (var j = 0; j < types.length; j++) {
            var t = types[j];
            var tn = String(t.getName());
            if (tn === "java.util.UUID") args[j] = bossId;
            else if (tn.indexOf("Component") >= 0) args[j] = comp;
            else if (tn === "float") args[j] = new java.lang.Float(progress);
            else if (tn === "boolean") args[j] = java.lang.Boolean.FALSE;
            else if (t.isEnum()) {
                args[j] = pickEnum(t, enumIndex === 0 ? "PURPLE" : "PROGRESS");
                enumIndex++;
            } else {
                ok = false;
                break;
            }
        }
        if (!ok) continue;
        return ctors[i].newInstance(args);
    }
    throw "no usable boss bar constructor";
}

function update() {
    var mc = Minecraft.getInstance();
    if (mc.player == null) { bossEvent = null; return; }

    var info = getInfo();
    var overlay = findOverlay(mc.gui);
    if (overlay == null) { fail("boss bar overlay not found. Send config/SushiTriggers/bossbar-debug.txt"); return; }
    var map = findMap(overlay);
    if (map == null) { fail("boss bar map not found"); return; }

    if (bossEvent == null || !map.containsKey(bossId)) {
        bossEvent = createEvent(info.component, info.progress);
        map.put(bossId, bossEvent);
    }

    bossEvent.setName(Component.literal(info.text));
    bossEvent.setProgress(info.progress);
}

function removeBar() {
    try {
        var overlay = findOverlay(Minecraft.getInstance().gui);
        var map = overlay != null ? findMap(overlay) : null;
        if (map != null) map.remove(bossId);
    } catch (e) {}
    bossEvent = null;
}

register("tick", function () {
    if (!enabled) return;
    tickCount++;
    if (tickCount % 5 !== 0) return;
    try {
        update();
    } catch (e) {
        fail("boss bar failed: " + e);
    }
});

register("gameUnload", function () { removeBar(); });

register("command", function (arg) {
    if (arg === "on") enabled = true;
    else if (arg === "off") enabled = false;
    else if (arg === "now") {
        ChatLib.chat(getInfo().text.replace(/§/g, "&"));
        return;
    } else enabled = !enabled;
    if (!enabled) removeBar();
    failed = false;
    ChatLib.chat("&5[DarkAuction] &f" + (enabled ? "on" : "off"));
}).setName("da");

ChatLib.chat("&5[DarkAuction] &7loaded (boss bar). /da [on|off|now]");
