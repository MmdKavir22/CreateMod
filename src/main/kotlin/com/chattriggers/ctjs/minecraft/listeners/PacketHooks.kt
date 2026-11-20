package com.chattriggers.ctjs.minecraft.listeners

import com.chattriggers.ctjs.triggers.TriggerType
import com.chattriggers.ctjs.utils.Refl
import net.minecraft.client.Minecraft

object PacketHooks {
    @JvmStatic
    fun onPacketReceived(packet: Any?): Boolean {
        if (packet == null) return false

        val mc = Minecraft.getInstance()
        if (Refl.call(mc, "isSameThread") == false) return false

        val event = CancellableEvent()
        TriggerType.PacketReceived.triggerAll(packet, event)
        return event.isCancelled()
    }
}
