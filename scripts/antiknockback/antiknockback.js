var Minecraft = Packages.net.minecraft.client.Minecraft;
var Vec3 = Packages.net.minecraft.world.phys.Vec3;

var enabled = true;
var lastVel = null;
var lastHealth = -1;
var cancelled = 0;
var restored = 0;

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
        cancelled++;
    }
});

register("tick", function () {
    var player = Minecraft.getInstance().player;
    if (player == null) { lastVel = null; return; }

    var v = player.getDeltaMovement();
    var health = Number(player.getHealth());

    if (enabled && lastVel != null && health < lastHealth) {
        var dx = v.x - lastVel[0];
        var dz = v.z - lastVel[2];
        if (Math.abs(dx) + Math.abs(dz) > 0.02 || v.y - lastVel[1] > 0.1) {
            player.setDeltaMovement(new Vec3(lastVel[0], lastVel[1], lastVel[2]));
            v = player.getDeltaMovement();
            restored++;
        }
    }

    lastVel = [v.x, v.y, v.z];
    lastHealth = health;
});

register("command", function (arg) {
    if (arg === "on") enabled = true;
    else if (arg === "off") enabled = false;
    else if (arg === "stats") {
        ChatLib.chat("&b[AntiKB] &fpackets cancelled: " + cancelled + ", tick restores: " + restored);
        return;
    } else enabled = !enabled;
    ChatLib.chat("&b[AntiKB] &f" + (enabled ? "on" : "off"));
}).setName("antikb");

ChatLib.chat("&b[AntiKB] &7loaded. /antikb [on|off|stats]");
