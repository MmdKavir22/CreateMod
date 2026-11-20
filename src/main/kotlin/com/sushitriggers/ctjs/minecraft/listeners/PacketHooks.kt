package com.sushitriggers.ctjs.minecraft.listeners

import com.sushitriggers.ctjs.triggers.TriggerType
import com.sushitriggers.ctjs.utils.Refl
import com.sushitriggers.ctjs.utils.console.CTLog
import com.sushitriggers.ctjs.utils.console.LogType
import net.minecraft.client.Minecraft

object PacketHooks {
    private var debugCount = 0

    @JvmStatic
    fun onPacketReceived(packet: Any?): Boolean {
        if (packet == null) return false

        val mc = Minecraft.getInstance()
        if (Refl.call(mc, "isSameThread") == false) return false

        val packetId = (Refl.get(packet, "id") as? Number)?.toInt()
        val playerId = (Refl.call(mc.player, "getId") as? Number)?.toInt()

        if (debugCount < 5 || (packetId != null && packetId == playerId)) {
            debugCount++
            CTLog.add(LogType.INFO, "[PACKET] velocity packet ${packet.javaClass.simpleName} id=$packetId player=$playerId")
        }

        val event = CancellableEvent()
        TriggerType.PacketReceived.triggerAll(packet, event)

        if (event.isCancelled()) {
            CTLog.add(LogType.INFO, "[PACKET] cancelled by script")
        }

        return event.isCancelled()
    }
}
