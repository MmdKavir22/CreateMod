var Minecraft = Packages.net.minecraft.client.Minecraft;

var enabled = true;

function read(obj, name) {
    var value = obj[name];
    if (typeof value === "function") value = obj[name]();
    return Number(value);
}

register("packetReceived", function (packet, event) {
    if (!enabled) return;

    var player = Minecraft.getInstance().player;
    if (player == null) return;

    if (read(packet, "id") === read(player, "id")) {
        cancel(event);
    }
});

register("command", function (arg) {
    if (arg === "on") enabled = true;
    else if (arg === "off") enabled = false;
    else enabled = !enabled;

    ChatLib.chat("&b[AntiKB] &f" + (enabled ? "on" : "off"));
}).setName("antikb");

ChatLib.chat("&b[AntiKB] &7loaded. /antikb [on|off]");
