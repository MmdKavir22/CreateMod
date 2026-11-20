// ChatLogger - logs ALL chat (players, system, server) + your own /commands
// Source of chat: the game's own logs/latest.log ("[CHAT]" lines)
// /chatlog            -> settings GUI
// /chatlog folder     -> open log folder
// /chatlog toggle|on|off
// /chatlog status | test | debug
// /chatlog set <key> <value>

var Minecraft = Packages.net.minecraft.client.Minecraft;
var Component = Packages.net.minecraft.network.chat.Component;
var Button = Packages.net.minecraft.client.gui.components.Button;
var EditBox = Packages.net.minecraft.client.gui.components.EditBox;
var ScreenClass = Packages.net.minecraft.client.gui.screens.Screen;
var JFile = Packages.java.io.File;
var JArray = Packages.java.lang.reflect.Array;
var OBJ_CLASS = Packages.java.lang.Class.forName("java.lang.Object");
var STR_CLASS = Packages.java.lang.Class.forName("java.lang.String");

var DEFAULTS = {
    enabled: true,
    showTime: true,
    showDate: false,
    use24h: true,
    showSeconds: true,
    perServer: true,
    perDay: true,
    logSingleplayer: true,
    logCommands: true,
    saveDir: "C:\\Users\\Kavir\\Desktop\\New folder"
};

var SUBCOMMANDS = ["gui", "folder", "toggle", "on", "off", "status", "test", "debug", "set"];

var cfg = {};
var k;
for (k in DEFAULTS) {
    if (DEFAULTS.hasOwnProperty(k)) cfg[k] = DEFAULTS[k];
}

var pendingOpen = false;
var pendingLoadMsg = true;
var errCount = {};
var written = 0;
var lastWriteInfo = "nothing written yet";
var SCREEN_CLASS = null;
var tickCount = 0;
var logPos = -1;
var logState = "not started";
var histPrev = null;
var histState = "not started";
var histMethodCache = null;

// ---------------------------------------------------------------- helpers

function mc() {
    try {
        return Minecraft.getInstance();
    } catch (e) {
        return null;
    }
}

function say(s) {
    try {
        ChatLib.chat(s);
    } catch (e) {
        print(String(s));
    }
}

function warn(where, e) {
    var key = where + ":" + String(e);
    errCount[key] = (errCount[key] || 0) + 1;
    if (errCount[key] <= 2) {
        print("[ChatLogger] error in " + where + ": " + String(e));
        say("&c[ChatLogger] error in " + where + ": " + String(e));
    }
}

function objArr(items) {
    var a = JArray.newInstance(OBJ_CLASS, items.length);
    var i;
    for (i = 0; i < items.length; i++) a[i] = items[i];
    return a;
}

function pad(n, l) {
    var s = String(n);
    while (s.length < l) s = "0" + s;
    return s;
}

function dateStr(d) {
    return d.getFullYear() + "-" + pad(d.getMonth() + 1, 2) + "-" + pad(d.getDate(), 2);
}

function timeStr(d) {
    var h = d.getHours();
    var suffix = "";
    if (!cfg.use24h) {
        suffix = h >= 12 ? " PM" : " AM";
        h = h % 12;
        if (h === 0) h = 12;
    }
    var s = pad(h, 2) + ":" + pad(d.getMinutes(), 2);
    if (cfg.showSeconds) s += ":" + pad(d.getSeconds(), 2);
    return s + suffix;
}

function prefix(d) {
    var parts = [];
    if (cfg.showDate) parts.push(dateStr(d));
    if (cfg.showTime) parts.push(timeStr(d));
    if (parts.length === 0) return "";
    return "[" + parts.join(" ") + "] ";
}

function sanitize(s) {
    var r = String(s).replace(/[^A-Za-z0-9._-]/g, "_").replace(/^\.+/, "_");
    if (r.length > 80) r = r.substring(0, 80);
    if (r === "") r = "unknown";
    return r;
}

function getFieldValue(obj, name) {
    try {
        var v = obj[name];
        if (v !== undefined && v !== null && typeof v !== "function") return v;
    } catch (e) {}
    var c = obj.getClass();
    while (c !== null) {
        try {
            var f = c.getDeclaredField(name);
            f.setAccessible(true);
            return f.get(obj);
        } catch (e2) {}
        c = c.getSuperclass();
    }
    return null;
}

function gameDir() {
    try {
        var m = mc();
        if (m) {
            var g = getFieldValue(m, "gameDirectory");
            if (g !== null) {
                if (g instanceof Packages.java.nio.file.Path) g = g.toFile();
                return g;
            }
        }
    } catch (e) {}
    return new JFile(String(Packages.java.lang.System.getProperty("user.dir")));
}

function baseDir() {
    var p = String(cfg.saveDir || "").replace(/^\s+|\s+$/g, "");
    p = p.replace(/^"+|"+$/g, "");
    if (p === "") return new JFile(gameDir(), "chatlogs");
    var f = new JFile(p);
    if (f.isAbsolute()) return f;
    return new JFile(gameDir(), p);
}

function writeText(file, text, append) {
    var parent = file.getParentFile();
    if (parent !== null && !parent.exists()) parent.mkdirs();
    var w = new Packages.java.io.OutputStreamWriter(
        new Packages.java.io.FileOutputStream(file, append), "UTF-8");
    try {
        w.write(text);
    } finally {
        w.close();
    }
}

function readText(file) {
    var bytes = Packages.java.nio.file.Files.readAllBytes(file.toPath());
    return String(new Packages.java.lang.String(bytes, "UTF-8"));
}

// ---------------------------------------------------------------- config

function cfgFile() {
    return new JFile(new JFile(gameDir(), "config"), "chatlogger.json");
}

function loadConfig() {
    try {
        var f = cfgFile();
        if (!f.exists()) return;
        var obj = JSON.parse(readText(f));
        var key;
        for (key in DEFAULTS) {
            if (DEFAULTS.hasOwnProperty(key) && obj.hasOwnProperty(key) &&
                typeof obj[key] === typeof DEFAULTS[key]) {
                cfg[key] = obj[key];
            }
        }
    } catch (e) {
        warn("loadConfig", e);
    }
}

function saveConfig() {
    try {
        writeText(cfgFile(), JSON.stringify(cfg, null, 2), false);
    } catch (e) {
        warn("saveConfig", e);
    }
}

// ---------------------------------------------------------------- server key

function serverInfo() {
    var m = mc();
    try {
        if (m.getSingleplayerServer() !== null) {
            var name = "world";
            try {
                name = String(m.getSingleplayerServer().getWorldData().getLevelName());
            } catch (e1) {}
            return { single: true, folder: "singleplayer_" + sanitize(name) };
        }
    } catch (e2) {}
    try {
        var sd = m.getCurrentServer();
        if (sd !== null && sd !== undefined) {
            var id = getFieldValue(sd, "ip");
            if (id === null) id = getFieldValue(sd, "name");
            if (id !== null) return { single: false, folder: sanitize(id) };
        }
    } catch (e3) {}
    return { single: false, folder: "unknown" };
}

// ---------------------------------------------------------------- writing

function writeBatch(texts) {
    var info = serverInfo();
    if (info.single && !cfg.logSingleplayer) return null;
    var d = new Date();
    var dir = baseDir();
    if (cfg.perServer) dir = new JFile(dir, info.folder);
    var name = cfg.perDay ? (dateStr(d) + ".log") : "all.log";
    var p = prefix(d);
    var lines = [];
    var i;
    for (i = 0; i < texts.length; i++) {
        lines.push(p + String(texts[i]));
    }
    var file = new JFile(dir, name);
    writeText(file, lines.join("\n") + "\n", true);
    written += texts.length;
    lastWriteInfo = String(file.getAbsolutePath());
    return file;
}

// ---------------------------------------------------------------- source 1: logs/latest.log

function pollLog() {
    var f = new JFile(new JFile(gameDir(), "logs"), "latest.log");
    if (!f.exists()) {
        logState = "latest.log not found at " + String(f.getAbsolutePath());
        return;
    }
    var len = f.length();
    if (logPos < 0 || !cfg.enabled) {
        logPos = len;
        logState = cfg.enabled ? "watching" : "paused";
        return;
    }
    if (len < logPos) logPos = 0;
    if (len === logPos) return;

    var n = len - logPos;
    if (n > 1048576) n = 1048576;
    var buf = JArray.newInstance(Packages.java.lang.Byte.TYPE, n);
    var got = 0;
    var raf = new Packages.java.io.RandomAccessFile(f, "r");
    try {
        raf.seek(logPos);
        got = raf.read(buf, 0, n);
    } finally {
        raf.close();
    }
    if (got <= 0) return;

    var last = -1;
    var i;
    for (i = got - 1; i >= 0; i--) {
        if (buf[i] === 10) {
            last = i;
            break;
        }
    }
    if (last < 0) return; // incomplete line, wait for more
    var text = String(new Packages.java.lang.String(buf, 0, last + 1, "UTF-8"));
    logPos += last + 1;
    logState = "watching";

    var lines = text.split("\n");
    var out = [];
    for (i = 0; i < lines.length; i++) {
        var idx = lines[i].indexOf("[CHAT] ");
        if (idx >= 0) {
            out.push(lines[i].substring(idx + 7).replace(/\r$/, ""));
        }
    }
    if (out.length > 0) writeBatch(out);
}

// ---------------------------------------------------------------- source 2: own commands (input history)

function toStrings(coll) {
    var arr = [];
    var it = coll.iterator();
    while (it.hasNext()) arr.push(String(it.next()));
    return arr;
}

function readHistory(m) {
    var i;
    var j;
    // a) CommandHistory object on Minecraft
    var fs = m.getClass().getDeclaredFields();
    for (i = 0; i < fs.length; i++) {
        if (String(fs[i].getType().getSimpleName()) === "CommandHistory") {
            fs[i].setAccessible(true);
            var ch = fs[i].get(m);
            if (ch === null) continue;
            var ms = ch.getClass().getDeclaredMethods();
            for (j = 0; j < ms.length; j++) {
                if (ms[j].getParameterCount() === 0 &&
                    Packages.java.util.Collection.class === undefined) {
                    continue;
                }
                if (ms[j].getParameterCount() === 0) {
                    var rt = String(ms[j].getReturnType().getName());
                    if (rt === "java.util.Collection" || rt === "java.util.List" ||
                        rt === "java.util.Deque" || rt === "java.lang.Iterable") {
                        ms[j].setAccessible(true);
                        var r = ms[j].invoke(ch, objArr([]));
                        if (r !== null) {
                            histState = "CommandHistory." + String(ms[j].getName());
                            return toStrings(r);
                        }
                    }
                }
            }
        }
    }
    // b) List<String> field in ChatComponent
    var gui = getFieldValue(m, "gui");
    if (gui !== null) {
        var chat = null;
        try {
            chat = gui.getChat();
        } catch (e) {}
        if (chat !== null && chat !== undefined) {
            var cf = chat.getClass().getDeclaredFields();
            for (i = 0; i < cf.length; i++) {
                var gt = String(cf[i].getGenericType().toString());
                if (gt.indexOf("List<java.lang.String>") !== -1) {
                    cf[i].setAccessible(true);
                    histState = "ChatComponent." + String(cf[i].getName());
                    return toStrings(cf[i].get(chat));
                }
            }
        }
    }
    histState = "no input history found";
    return null;
}

function pollHistory() {
    var m = mc();
    if (!m) return;
    var snap = readHistory(m);
    if (snap === null) return;
    var i;
    if (histPrev === null || !cfg.enabled || !cfg.logCommands) {
        histPrev = {};
        for (i = 0; i < snap.length; i++) histPrev[snap[i]] = true;
        return;
    }
    var fresh = [];
    for (i = 0; i < snap.length; i++) {
        if (!histPrev.hasOwnProperty(snap[i]) && snap[i].charAt(0) === "/") {
            fresh.push(">> " + snap[i]);
        }
    }
    histPrev = {};
    for (i = 0; i < snap.length; i++) histPrev[snap[i]] = true;
    if (fresh.length > 0) writeBatch(fresh);
}

// ---------------------------------------------------------------- reflection helpers (GUI)

function callMethod(obj, method, arg) {
    method.setAccessible(true);
    return method.invoke(obj, objArr([arg]));
}

function findScreenSetter(m) {
    if (SCREEN_CLASS === null) return null;
    var ms = m.getClass().getDeclaredMethods();
    var i;
    for (i = 0; i < ms.length; i++) {
        var n = String(ms[i].getName());
        if (ms[i].getParameterCount() === 1 &&
            ms[i].getParameterTypes()[0].isAssignableFrom(SCREEN_CLASS) &&
            n.toLowerCase().indexOf("screen") !== -1 &&
            String(ms[i].getReturnType().getName()) === "void") {
            return ms[i];
        }
    }
    return null;
}

function showScreen(m, screen) {
    var names = ["setScreen", "setScreenAndShow"];
    var i;
    for (i = 0; i < names.length; i++) {
        if (typeof m[names[i]] === "function") {
            m[names[i]](screen);
            return;
        }
    }
    var setter = findScreenSetter(m);
    if (setter === null) throw "no screen setter found (use /chatlog debug)";
    callMethod(m, setter, screen);
}

function addWidget(screen, w) {
    var ms = SCREEN_CLASS.getDeclaredMethods();
    var i;
    for (i = 0; i < ms.length; i++) {
        if (String(ms[i].getName()) === "addRenderableWidget" && ms[i].getParameterCount() === 1) {
            callMethod(screen, ms[i], w);
            return;
        }
    }
    throw "addRenderableWidget not found (use /chatlog debug)";
}

// ---------------------------------------------------------------- open folder / test

function openFolder() {
    var dir = baseDir();
    if (!dir.exists()) dir.mkdirs();
    var path = String(dir.getAbsolutePath());
    var os = String(Packages.java.lang.System.getProperty("os.name")).toLowerCase();
    var cmd;
    if (os.indexOf("win") !== -1) cmd = ["explorer.exe", path];
    else if (os.indexOf("mac") !== -1) cmd = ["open", path];
    else cmd = ["xdg-open", path];

    try {
        var arr = JArray.newInstance(STR_CLASS, cmd.length);
        var i;
        for (i = 0; i < cmd.length; i++) arr[i] = cmd[i];
        new Packages.java.lang.ProcessBuilder(arr).start();
        return path;
    } catch (e) {
        warn("openFolder(process)", e);
    }
    try {
        Packages.java.awt.Desktop.getDesktop().open(dir);
        return path;
    } catch (e2) {
        warn("openFolder(desktop)", e2);
    }
    return null;
}

function doTest() {
    var f = writeBatch(["ChatLogger test line"]);
    if (f === null) return "nothing written (singleplayer logging is off)";
    if (!f.exists()) return "write finished but file does not exist: " + String(f.getAbsolutePath());
    return "OK -> " + String(f.getAbsolutePath());
}

// ---------------------------------------------------------------- GUI

function onOff(v) {
    return v ? "ON" : "OFF";
}

function makeToggle(key, label, x, y, w) {
    return Button.builder(Component.literal(label + ": " + onOff(cfg[key])), function (b) {
        cfg[key] = !cfg[key];
        b.setMessage(Component.literal(label + ": " + onOff(cfg[key])));
        saveConfig();
    }).bounds(x, y, w, 20).build();
}

var TOGGLES = [
    ["enabled", "Logging"],
    ["logCommands", "Log my /commands"],
    ["showTime", "Show time"],
    ["showDate", "Date in line"],
    ["use24h", "24h clock"],
    ["showSeconds", "Seconds"],
    ["perServer", "Folder per server"],
    ["perDay", "File per day"],
    ["logSingleplayer", "Singleplayer"]
];

function openSettings() {
    var m = mc();
    var editBox = null;

    function applyEdit() {
        if (editBox !== null) {
            cfg.saveDir = String(editBox.getValue()).replace(/^\s+|\s+$/g, "");
        }
        saveConfig();
    }

    var screen = new JavaAdapter(ScreenClass, {
        removed: function () {
            try {
                applyEdit();
            } catch (e) {
                warn("removed", e);
            }
        }
    }, Component.literal("Chat Logger Settings"));

    SCREEN_CLASS = screen.getClass().getSuperclass();

    showScreen(m, screen);

    var cx = Math.floor(screen.width / 2);
    var top = Math.floor(screen.height / 2) - 115;
    var colW = 150;
    var leftX = cx - 155;
    var rightX = cx + 5;

    var title = Button.builder(Component.literal("Chat Logger Settings"), function (b) {})
        .bounds(cx - 155, top, 310, 20).build();
    title.active = false;
    addWidget(screen, title);

    var i;
    for (i = 0; i < TOGGLES.length; i++) {
        var col = i % 2;
        var row = Math.floor(i / 2);
        addWidget(screen, makeToggle(TOGGLES[i][0], TOGGLES[i][1],
            col === 0 ? leftX : rightX, top + 28 + row * 24, colW));
    }

    editBox = new EditBox(m.font, cx - 155, top + 156, 310, 20, Component.literal("Save folder"));
    editBox.setMaxLength(256);
    editBox.setValue(String(cfg.saveDir || ""));
    if (typeof editBox.setHint === "function") {
        editBox.setHint(Component.literal("Save folder (empty = game folder/chatlogs)"));
    }
    addWidget(screen, editBox);

    var openBtn = Button.builder(Component.literal("Open log folder"), function (b) {
        try {
            applyEdit();
            var p = openFolder();
            if (p !== null) say("&a[ChatLogger] &7" + p);
            else say("&c[ChatLogger] could not open the folder");
        } catch (e) {
            warn("openBtn", e);
        }
    }).bounds(leftX, top + 184, colW, 20).build();
    addWidget(screen, openBtn);

    var testBtn = Button.builder(Component.literal("Test write"), function (b) {
        try {
            applyEdit();
            var r = doTest();
            b.setMessage(Component.literal(r.indexOf("OK") === 0 ? "Test: OK" : "Test: FAILED"));
            say("&7[ChatLogger] test: " + r);
        } catch (e) {
            b.setMessage(Component.literal("Test: FAILED"));
            say("&c[ChatLogger] test error: " + String(e));
        }
    }).bounds(rightX, top + 184, colW, 20).build();
    addWidget(screen, testBtn);

    var done = Button.builder(Component.literal("Done"), function (b) {
        applyEdit();
        showScreen(Minecraft.getInstance(), null);
    }).bounds(cx - 100, top + 212, 200, 20).build();
    addWidget(screen, done);
}

// ---------------------------------------------------------------- triggers

register("gameLoad", function () {
    loadConfig();
    print("[ChatLogger] loaded. Use /chatlog");
});

register("gameUnload", function () {
    saveConfig();
});

register("tick", function () {
    try {
        var m = mc();
        if (!m) return;

        if (pendingLoadMsg && m.player) {
            pendingLoadMsg = false;
            say("&a[ChatLogger] &7loaded (" + (cfg.enabled ? "ON" : "OFF") + ") - /chatlog");
            try {
                if (cfg.enabled) writeBatch(["=== ChatLogger started ==="]);
            } catch (e0) {
                warn("startLine", e0);
            }
        }

        if (pendingOpen) {
            pendingOpen = false;
            try {
                openSettings();
            } catch (e) {
                say("&c[ChatLogger] GUI failed: " + String(e) + " - use /chatlog set <key> <value>");
            }
        }

        tickCount++;
        if (tickCount % 5 === 0) {
            try {
                pollLog();
            } catch (e1) {
                warn("pollLog", e1);
            }
            try {
                pollHistory();
            } catch (e2) {
                warn("pollHistory", e2);
            }
        }
    } catch (e3) {
        warn("tick", e3);
    }
});

function parseBool(s) {
    s = String(s).toLowerCase();
    return s === "on" || s === "true" || s === "1" || s === "yes";
}

function setEnabled(v) {
    cfg.enabled = v;
    saveConfig();
    say("&a[ChatLogger] " + (cfg.enabled ? "&aON" : "&cOFF"));
}

function runCommand(a) {
    if (a.length === 0) {
        pendingOpen = true;
        return;
    }
    var sub = a[0].toLowerCase();
    var j;
    if (sub === "gui" || sub === "settings") {
        pendingOpen = true;
    } else if (sub === "folder" || sub === "open") {
        var p = openFolder();
        say(p !== null ? "&a[ChatLogger] &7" + p : "&c[ChatLogger] could not open the folder");
    } else if (sub === "toggle") {
        setEnabled(!cfg.enabled);
    } else if (sub === "on") {
        setEnabled(true);
    } else if (sub === "off") {
        setEnabled(false);
    } else if (sub === "status") {
        say("&7enabled=" + cfg.enabled + " commands=" + cfg.logCommands + " perServer=" +
            cfg.perServer + " perDay=" + cfg.perDay);
        say("&7dir=" + String(baseDir().getAbsolutePath()));
        say("&7lines written=" + written + " | last file: " + lastWriteInfo);
        say("&7chat source: " + logState + " | commands source: " + histState);
    } else if (sub === "test") {
        say("&7[ChatLogger] test: " + doTest());
    } else if (sub === "debug") {
        var m = mc();
        print("[ChatLogger] gameDir=" + String(gameDir().getAbsolutePath()));
        print("[ChatLogger] baseDir=" + String(baseDir().getAbsolutePath()));
        print("[ChatLogger] logState=" + logState + " logPos=" + logPos);
        print("[ChatLogger] histState=" + histState);
        var mf = m.getClass().getDeclaredFields();
        for (j = 0; j < mf.length; j++) {
            var tn = String(mf[j].getType().getSimpleName());
            if (tn === "Gui" || tn === "CommandHistory" || tn === "File" || tn === "Path") {
                print("[ChatLogger] MC field: " + String(mf[j].getName()) + " : " + tn);
            }
        }
        say("&7debug info printed to the log");
    } else if (sub === "set" && a.length >= 3) {
        var key = a[1];
        if (!DEFAULTS.hasOwnProperty(key)) {
            say("&c[ChatLogger] unknown key. keys: " + Object.keys(DEFAULTS).join(", "));
            return;
        }
        if (typeof DEFAULTS[key] === "boolean") {
            cfg[key] = parseBool(a[2]);
        } else {
            cfg[key] = a.slice(2).join(" ");
        }
        saveConfig();
        say("&a[ChatLogger] " + key + " = " + cfg[key]);
    } else {
        say("&7/chatlog [" + SUBCOMMANDS.join(" | ") + "]");
    }
}

var cmdTrigger = register("command", function () {
    var a = [];
    var i;
    for (i = 0; i < arguments.length; i++) a.push(String(arguments[i]));
    try {
        runCommand(a);
    } catch (e) {
        say("&c[ChatLogger] error: " + String(e));
        print("[ChatLogger] command error: " + String(e));
    }
});
cmdTrigger.setName("chatlog");

try {
    if (typeof cmdTrigger.setTabCompletions === "function") {
        try {
            cmdTrigger.setTabCompletions.apply(cmdTrigger, SUBCOMMANDS);
        } catch (e1) {
            cmdTrigger.setTabCompletions(SUBCOMMANDS);
        }
    }
} catch (e2) {
    print("[ChatLogger] tab completion error: " + String(e2));
}