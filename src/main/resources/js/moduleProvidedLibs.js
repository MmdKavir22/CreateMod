(function(global) {
    const getClassName = path => path.substring(path.lastIndexOf('.') + 1)

    function getClass(path) {
        let clazz;
        if (typeof Java !== 'undefined' && typeof Java.type === 'function') {
            clazz = Java.type(path);
        } else {
            var parts = path.split('.');
            clazz = Packages;
            for (var i = 0; i < parts.length; i++)
                clazz = clazz[parts[i]];
        }

        if (clazz == null || String(clazz).startsWith('[JavaPackage'))
            throw new Error(`moduleProvidedLibs: Could not load class "${path}"`);

        return clazz;
    }

    function loadClass(path, className = getClassName(path)) {
        global[className] = getClass(path);
    }

    function loadInstance(path, className = getClassName(path)) {
        global[className] = getClass(path).INSTANCE;
    }

    if (typeof global.print !== 'function') {
        const ctLog = getClass("com.sushitriggers.ctjs.utils.console.CTLog").INSTANCE;
        global.print = function () {
            var out = [];
            for (var i = 0; i < arguments.length; i++)
                out.push(String(arguments[i]));
            ctLog.info(out.join(" "));
        };
    }

    // Extra libs
    loadClass("java.util.ArrayList");
    loadClass("java.util.HashMap");

    // Libraries
    loadClass("com.sushitriggers.ctjs.minecraft.libs.ChatLib");
    loadClass("com.sushitriggers.ctjs.minecraft.libs.FileLib");
    loadClass("com.sushitriggers.ctjs.minecraft.libs.MathLib");
    loadClass("com.sushitriggers.ctjs.minecraft.libs.HudOverlay", "Hud");
    loadClass("com.sushitriggers.ctjs.minecraft.libs.PlayerLib", "Player");
    loadClass("com.sushitriggers.ctjs.minecraft.libs.WorldLib", "World");
    loadClass("com.sushitriggers.ctjs.minecraft.libs.ClientLib", "Client");
    loadClass("com.sushitriggers.ctjs.minecraft.libs.ContainerLib", "Container");

    // Triggers
    loadInstance("com.sushitriggers.ctjs.engine.langs.js.JSRegister", "TriggerRegister");

    loadClass("com.sushitriggers.ctjs.triggers.CommandTrigger", "OnCommandTrigger");
    loadClass("com.sushitriggers.ctjs.triggers.RegularTrigger", "OnRegularTrigger");
    loadClass("com.sushitriggers.ctjs.triggers.Trigger", "OnTrigger");
    try { global.Priority = OnTrigger.Priority; } catch (e) {}

    // Misc
    loadInstance("com.sushitriggers.ctjs.utils.Config");
    loadClass("com.sushitriggers.ctjs.Reference", "SushiTriggers");
    try { global.ChatTriggers = global.SushiTriggers; } catch (e) {}

    global.cancel = function (event) {
        event.setCanceled(true);
    };

    global.register = (type, method) => TriggerRegister.register(type, method);

    // String prototypes
    String.prototype.addFormatting = function () {
        return ChatLib.addColor(this);
    };

    String.prototype.addColor = String.prototype.addFormatting;

    String.prototype.removeFormatting = function () {
        return ChatLib.removeFormatting(this);
    };

    // animation
    global.easeOut = function (start, finish, speed, jump) {
        if (jump === undefined) jump = 1;
        if (Math.floor(Math.abs(finish - start) / jump) > 0)
            return start + (finish - start) / speed;
        return finish;
    };

    Number.prototype.easeOut = function (to, speed, jump) {
        return easeOut(this, to, speed, jump);
    };

})(this);
