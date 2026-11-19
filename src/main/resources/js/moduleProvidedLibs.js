(function(global) {
    const Class = Java.type('java.lang.Class');

    const getClassName = path => path.substring(path.lastIndexOf('.') + 1)

    function getClass(path) {
        const clazz = Java.type(path);
        if (clazz.class instanceof Class)
            return clazz;
        throw new Error(`moduleProvidedLibs: Could not load class "${path}"`);
    }

    function loadClass(path, className = getClassName(path)) {
        global[className] = getClass(path);
    }

    function loadInstance(path, className = getClassName(path)) {
        global[className] = getClass(path).INSTANCE;
    }

    // Extra libs
    loadClass("java.util.ArrayList");
    loadClass("java.util.HashMap");

    // Libraries
    loadClass("com.chattriggers.ctjs.minecraft.libs.ChatLib");
    loadClass("com.chattriggers.ctjs.minecraft.libs.FileLib");
    loadClass("com.chattriggers.ctjs.minecraft.libs.MathLib");

    // Triggers
    loadInstance("com.chattriggers.ctjs.engine.langs.js.JSRegister", "TriggerRegister");

    loadClass("com.chattriggers.ctjs.triggers.CommandTrigger", "OnCommandTrigger");
    loadClass("com.chattriggers.ctjs.triggers.RegularTrigger", "OnRegularTrigger");
    loadClass("com.chattriggers.ctjs.triggers.Trigger", "OnTrigger");
    global.Priority = OnTrigger.Priority;

    // Misc
    loadInstance("com.chattriggers.ctjs.utils.Config");
    loadClass("com.chattriggers.ctjs.Reference", "ChatTriggers");

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
    global.easeOut = (start, finish, speed, jump = 1) => {
        if (Math.floor(Math.abs(finish - start) / jump) > 0)
            return start + (finish - start) / speed;
        return finish;
    };

    Number.prototype.easeOut = function (to, speed, jump) {
        return easeOut(this, to, speed, jump);
    };

})(this);
