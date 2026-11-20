var Minecraft = Packages.net.minecraft.client.Minecraft;

var enabled = true;
var keep = 0;
var watch = 0;
var prevVel = null;
var prevHealth = null;
var prevHurt = 0;

function read(obj, name) {
    var value = obj[name];
    if (typeof value === "function") value = obj[name]();
    return Number(value);
}

function getVel(player) {
    var v = player.getDeltaMovement();
    return { x: read(v, "x"), y: read(v, "y"), z: read(v, "z") };
}

function getHurt(player) {
    try {
        var h = player.hurtTime;
        if (typeof h === "function") h = player.hurtTime();
        h = Number(h);
        return isNaN(h) ? 0 : h;
    } catch (e) {
        return 0;
    }
}

register("tick", function () {
    var player = Minecraft.getInstance().player;

    if (player == null) {
        prevVel = null;
        prevHealth = null;
        prevHurt = 0;
        watch = 0;
        return;
    }

    var vel = getVel(player);
    var health = Number(player.getHealth());
    var hurt = getHurt(player);

    if (prevHealth !== null && (health < prevHealth || (hurt > prevHurt && prevHurt === 0))) {
        watch = 3;
    }

    if (enabled && watch > 0 && prevVel !== null) {
        var dx = vel.x - prevVel.x;
        var dy = vel.y - prevVel.y;
        var dz = vel.z - prevVel.z;
        var horizontal = Math.sqrt(dx * dx + dz * dz);

        if (horizontal > 0.08 || dy > 0.15) {
            player.setDeltaMovement(
                prevVel.x + dx * keep,
                prevVel.y + dy * keep,
                prevVel.z + dz * keep
            );
            vel = getVel(player);
            watch = 0;
        }
    }

    if (watch > 0) watch--;

    prevVel = vel;
    prevHealth = health;
    prevHurt = hurt;
});

register("command", function (arg) {
    if (arg === undefined) {
        enabled = !enabled;
    } else if (arg === "on") {
        enabled = true;
    } else if (arg === "off") {
        enabled = false;
    } else if (!isNaN(Number(arg))) {
        keep = Math.max(0, Math.min(100, Number(arg))) / 100;
        enabled = true;
    }

    ChatLib.chat("&b[AntiKB] &f" + (enabled ? "on" : "off") + " &7- knockback kept: &f" + Math.round(keep * 100) + "%");
}).setName("antikb");

ChatLib.chat("&b[AntiKB] &7loaded. /antikb [on|off|0-100]");
