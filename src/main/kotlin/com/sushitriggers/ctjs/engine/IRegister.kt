package com.sushitriggers.ctjs.engine

import com.sushitriggers.ctjs.triggers.*

@Suppress("unused")
interface IRegister {
    fun getImplementationLoader(): ILoader

    /**
     * Helper method register a trigger.
     *
     * Called by taking the original name of the method, i.e. `registerChat`,
     * removing the word register, and comparing it case-insensitively with
     * the methods below.
     */
    fun register(triggerType: Any, method: Any): Trigger {
        require(triggerType is String) {
            "register() expects a String as its first argument"
        }

        val name = "register" + triggerType.lowercase()

        val func = this::class.java.methods.firstOrNull {
            it.name.lowercase() == name && it.parameterCount == 1
        }

        if (func != null) return func.invoke(this, method) as Trigger

        val type = TriggerType.values().firstOrNull { it.name.equals(triggerType, ignoreCase = true) }
            ?: TriggerType.Other

        return RegularTrigger(method, type, getImplementationLoader())
    }

    fun registerTick(method: Any): RegularTrigger {
        return RegularTrigger(method, TriggerType.Tick, getImplementationLoader())
    }

    fun registerGameLoad(method: Any): RegularTrigger {
        return RegularTrigger(method, TriggerType.GameLoad, getImplementationLoader())
    }

    fun registerGameUnload(method: Any): RegularTrigger {
        return RegularTrigger(method, TriggerType.GameUnload, getImplementationLoader())
    }

    fun registerCommand(method: Any): CommandTrigger {
        return CommandTrigger(method, getImplementationLoader())
    }

    /**
     * Registers a new trigger that runs whenever the client receives a velocity
     * (knockback) packet. NOTE: for now this trigger only fires for entity
     * motion packets; more packet types will be added later.
     *
     * Passes through two arguments:
     * - The packet
     * - The event, which can be cancelled with `cancel(event)`
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    fun registerPacketReceived(method: Any): RegularTrigger {
        return RegularTrigger(method, TriggerType.PacketReceived, getImplementationLoader())
    }
}
