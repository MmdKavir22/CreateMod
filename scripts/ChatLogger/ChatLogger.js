// ChatLogger - logs all chat messages to files (per server, per day)
// Command: /chatlog            -> toggle on/off
//          /chatlog gui        -> settings screen
//          /chatlog status     -> show current settings
//          /chatlog debug      -> print screen-related methods to the log
//          /chatlog set <key> <value>  -> fallback if the GUI doesn't work

var Minecraft = Packages.net.minecraft.client.Minecraft;
var Component = Packages.net.minecraft.network.chat.Component;
var Button = Packages.net.minecraft.client.gui.components.Button;
var EditBox = Packages.net.minecraft.client.gui.components.EditBox;
var ScreenClass = Packages.net.minecraft.client.gui.screens.Screen;
var JFile = Packages.java.io.File;

var DEFAULTS = {
    enabled: true,
    showTime: true,
    showDate: false,
    use24h: true,
    showSeconds: true,
    perServer: true,
    perDay: true,
    logSingleplayer: true,
    saveDir: ""
};

var cfg = {};
var k;
for (k in DEFAULTS) {
    if (DEFAULTS.hasOwnProperty(k)) cfg[k] = DEFAULTS[k];
}

var pendingOpen = false;
var pendingLoadMsg = true;
var needBaseline = true;
var lastRef = null;
var lastHash = 0;
var chatList = null;
var chatHash = 0;
var errCount = {};
var SCREEN_CLASS = null; // java.lang.Class of Screen (taken from an instance)

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
    }
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

function idHash(o) {
    return Packages.java.lang.System.identityHashCode(o);
}

function gameDir() {
    try {
        var m = mc();
        if (m && m.gameDirectory) return m.gameDirectory;
    } catch (e) {}
    return new JFile(String(Packages.java.lang.System.getProperty("user.dir")));
}

function baseDir() {
    var p = String(cfg.saveDir || "").replace(/^\s+|\s+$/g, "");
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
            var id = (typeof sd.ip !== "undefined") ? sd.ip : sd.name;
            return { single: false, folder: sanitize(id) };
        }
    } catch (e3) {}
    return { single: false, folder: "unknown" };
}

// ---------------------------------------------------------------- chat reading

function getChatList() {
    var m = mc();
    if (!m || !m.gui) return null;
    var chat = m.gui.getChat();
    if (chat === null || chat === undefined) return null;
    var h = idHash(chat);
    if (chatList !== null && h === chatHash) return chatList;

    var cls = chat.getClass();
    var list = null;
    try {
        var f = cls.getDeclaredField("allMessages");
        f.setAccessible(true);
        list = f.get(chat);
    } catch (e) {
        // fallback: find a java.util.List field whose elements are GuiMessage
        var fields = cls.getDeclaredFields();
        var i;
        for (i = 0; i < fields.length; i++) {
            try {
                fields[i].setAccessible(true);
                var cand = fields[i].get(chat);
                if (cand !== null && cand instanceof Packages.java.util.List &&
                    cand.size() > 0 &&
                    String(cand.get(0).getClass().getSimpleName()) === "GuiMessage") {
                    list = cand;
                    break;
                }
            } catch (e2) {}
        }
    }
    if (list === null || list === undefined) return null;
    chatList = list;
    chatHash = h;
    return chatList;
}

function msgText(m) {
    try {
        var c = (typeof m.content === "function") ? m.content() : m.content;
        if (c !== null && c !== undefined) return String(c.getString());
    } catch (e) {}
    return String(m);
}

function writeBatch(texts) {
    var info = serverInfo();
    if (info.single && !cfg.logSingleplayer) return;
    var d = new Date();
    var dir = baseDir();
    if (cfg.perServer) dir = new JFile(dir, info.folder);
    var name = cfg.perDay ? (dateStr(d) + ".log") : "all.log";
    var p = prefix(d);
    var lines = [];
    var i;
    for (i = 0; i < texts.length; i++) {
        lines.push(p + String(texts[i]).replace(/\r?\n/g, " \u23CE "));
    }
    writeText(new JFile(dir, name), lines.join("\n") + "\n", true);
}

function pollChat() {
    if (!cfg.enabled) {
        needBaseline = true;
        return;
    }
    var list = getChatList();
    if (list === null) return;
    var n = list.size();
    if (n === 0) {
        lastRef = null;
        lastHash = 0;
        needBaseline = false;
        return;
    }
    var newest = list.get(0);
    var nh = idHash(newest);
    if (needBaseline) {
        lastRef = newest;
        lastHash = nh;
        needBaseline = false;
        return;
    }
    if (lastRef !== null && nh === lastHash) return;

    var fresh = [];
    var i;
    for (i = 0; i < n; i++) {
        var item = list.get(i);
        if (lastRef !== null && idHash(item) === lastHash) break;
        fresh.push(item);
    }
    lastRef = newest;
    lastHash = nh;

    var texts = [];
    for (i = fresh.length - 1; i >= 0; i--) {
        texts.push(msgText(fresh[i]));
    }
    if (texts.length > 0) writeBatch(texts);
}

// ---------------------------------------------------------------- reflection helpers

function callMethod(obj, method, arg) {
    method.setAccessible(true);
    return method.invoke(obj, [arg]);
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

    // real Screen class, taken from the instance (superclass of the adapter)
    SCREEN_CLASS = screen.getClass().getSuperclass();

    showScreen(m, screen);

    var cx = Math.floor(screen.width / 2);
    var top = Math.floor(screen.height / 2) - 100;
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

    editBox = new EditBox(m.font, cx - 155, top + 132, 310, 20, Component.literal("Save folder"));
    editBox.setMaxLength(256);
    editBox.setValue(String(cfg.saveDir || ""));
    if (typeof editBox.setHint === "function") {
        editBox.setHint(Component.literal("Save folder (empty = .minecraft/chatlogs)"));
    }
    addWidget(screen, editBox);

    var done = Button.builder(Component.literal("Done"), function (b) {
        applyEdit();
        showScreen(Minecraft.getInstance(), null);
    }).bounds(cx - 100, top + 160, 200, 20).build();
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
        }

        if (pendingOpen) {
            pendingOpen = false;
            try {
                openSettings();
            } catch (e) {
                say("&c[ChatLogger] GUI failed: " + String(e) + " - use /chatlog set <key> <value>");
            }
        }

        pollChat();
    } catch (e2) {
        warn("tick", e2);
    }
});

function parseBool(s) {
    s = String(s).toLowerCase();
    return s === "on" || s === "true" || s === "1" || s === "yes";
}

register("command", function () {
    var a = [];
    var i;
    for (i = 0; i < arguments.length; i++) a.push(String(arguments[i]));

    try {
        if (a.length === 0) {
            cfg.enabled = !cfg.enabled;
            saveConfig();
            say("&a[ChatLogger] " + (cfg.enabled ? "&aON" : "&cOFF"));
            return;
        }
        var sub = a[0].toLowerCase();
        if (sub === "gui" || sub === "settings") {
            pendingOpen = true;
        } else if (sub === "status") {
            say("&7enabled=" + cfg.enabled + " time=" + cfg.showTime + " date=" + cfg.showDate +
                " 24h=" + cfg.use24h + " sec=" + cfg.showSeconds + " perServer=" + cfg.perServer +
                " perDay=" + cfg.perDay + " single=" + cfg.logSingleplayer);
            say("&7dir=" + String(baseDir().getAbsolutePath()));
        } else if (sub === "debug") {
            var found = 0;
            var j;
            var ms = mc().getClass().getDeclaredMethods();
            for (j = 0; j < ms.length; j++) {
                if (String(ms[j].getName()).toLowerCase().indexOf("screen") !== -1) {
                    print("[ChatLogger] MC: " + String(ms[j].toString()));
                    found++;
                }
            }
            if (SCREEN_CLASS !== null) {
                var sm = SCREEN_CLASS.getDeclaredMethods();
                for (j = 0; j < sm.length; j++) {
                    if (String(sm[j].getName()).toLowerCase().indexOf("widget") !== -1) {
                        print("[ChatLogger] Screen: " + String(sm[j].toString()));
                        found++;
                    }
                }
            }
            say("&7" + found + " methods printed to the log");
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
            say("&7/chatlog | gui | status | debug | set <key> <value>");
        }
    } catch (e) {
        warn("command", e);
    }
}).setName("chatlog");