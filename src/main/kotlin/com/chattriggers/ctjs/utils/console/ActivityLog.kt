package com.chattriggers.ctjs.utils.console

import com.chattriggers.ctjs.minecraft.libs.ChatLib
import com.chattriggers.ctjs.utils.Config
import com.chattriggers.ctjs.utils.Refl
import net.minecraft.client.Minecraft
import kotlin.math.hypot
import kotlin.math.sqrt

object ActivityLog {
    private var hadPlayer = false
    private var lastLevel: Any? = null
    private var lastDimension: String? = null
    private var lastHealth: Double? = null
    private var lastAbsorption: Double? = null
    private var lastFood: Double? = null
    private var lastHurt = 0.0
    private var lastVel: DoubleArray? = null
    private var lastPos: DoubleArray? = null
    private var lastOnGround: Boolean? = null
    private var lastSprint = false
    private var lastSneak = false
    private var lastFire = false
    private var lastItem: String? = null
    private var lastScreen: String? = null
    private var watchKnockback = 0
    private var dead = false

    val categories = listOf("health", "knockback", "movement", "items", "screens", "world")

    fun categoryOf(text: String): String = when {
        text.startsWith("DAMAGE") || text.startsWith("HEAL") || text.startsWith("HURT") ||
                text.startsWith("ABSORPTION") || text.startsWith("FOOD") || text.startsWith("DEAD") ||
                text.startsWith("ON FIRE") || text.startsWith("FIRE") -> "health"
        text.startsWith("KNOCKBACK") || text.startsWith("VELOCITY") -> "knockback"
        text.startsWith("JUMP") || text.startsWith("SPRINT") || text.startsWith("SNEAK") ||
                text.startsWith("TELEPORT") -> "movement"
        text.startsWith("HELD") -> "items"
        text.startsWith("SCREEN") -> "screens"
        else -> "world"
    }

    fun event(text: String) {
        if (categoryOf(text) in Config.disabledEventCategories) return
        if (Config.logEvents) CTLog.add(LogType.INFO, "[EVENT] $text")
        if (Config.eventsInChat) ChatLib.chat("&8[EVENT] &7$text")
    }

    private fun fmt(d: Double) = String.format("%.2f", d)

    private fun reset() {
        lastLevel = null
        lastDimension = null
        lastHealth = null
        lastAbsorption = null
        lastFood = null
        lastHurt = 0.0
        lastVel = null
        lastPos = null
        lastOnGround = null
        lastSprint = false
        lastSneak = false
        lastFire = false
        lastItem = null
        watchKnockback = 0
        dead = false
    }

    fun tick() {
        if (!Config.logEvents && !Config.eventsInChat) return

        val mc = Minecraft.getInstance()
        val player = mc.player

        val screenName = Refl.get(mc, "screen")?.javaClass?.simpleName
        if (screenName != lastScreen) {
            if (screenName != null) event("SCREEN opened: $screenName")
            else if (lastScreen != null) event("SCREEN closed: $lastScreen")
            lastScreen = screenName
        }

        if (player == null) {
            if (hadPlayer) {
                event("LEFT world")
                hadPlayer = false
                reset()
            }
            return
        }

        val level = Refl.get(mc, "level")
        val dimension = Refl.call(level, "dimension")?.toString()?.substringAfterLast("/")?.trimEnd(']')?.trim()

        if (!hadPlayer) {
            event("JOINED world, dimension=$dimension")
            hadPlayer = true
        } else if (level !== lastLevel && lastLevel != null) {
            event("WORLD changed: dimension=$dimension")
            lastPos = null
            lastVel = null
        }
        lastLevel = level
        lastDimension = dimension

        val health = Refl.num(player, "health")
        val absorption = Refl.num(player, "absorptionAmount")
        val food = Refl.num(Refl.call(player, "getFoodData"), "foodLevel")
        val hurt = Refl.num(player, "hurtTime") ?: (Refl.field(player, "hurtTime") as? Number)?.toDouble() ?: 0.0

        val velObj = Refl.call(player, "getDeltaMovement")
        val vel = if (velObj != null) doubleArrayOf(
            Refl.num(velObj, "x") ?: 0.0, Refl.num(velObj, "y") ?: 0.0, Refl.num(velObj, "z") ?: 0.0
        ) else null
        val pos = doubleArrayOf(Refl.num(player, "x") ?: 0.0, Refl.num(player, "y") ?: 0.0, Refl.num(player, "z") ?: 0.0)

        var hurtEvent = false

        if (health != null && lastHealth != null && health != lastHealth) {
            val diff = health - lastHealth!!
            if (diff < 0) {
                val source = Refl.call(player, "getLastDamageSource")
                val type = Refl.call(source, "getMsgId")?.toString()
                    ?: Refl.call(Refl.call(source, "type"), "msgId")?.toString()
                    ?: source?.toString()
                val attacker = Refl.call(source, "getEntity", "getCausingEntity")
                val attackerName = Refl.text(Refl.call(attacker, "getName")) ?: attacker?.javaClass?.simpleName
                val direct = Refl.call(source, "getDirectEntity")
                val directName = if (direct != null && direct !== attacker) direct.javaClass.simpleName else null

                event(
                    "DAMAGE ${fmt(diff)} (health ${fmt(lastHealth!!)} -> ${fmt(health)})" +
                            (type?.let { " source=$it" } ?: "") +
                            (attackerName?.let { " attacker=$it" } ?: "") +
                            (directName?.let { " via=$it" } ?: "")
                )
                hurtEvent = true
            } else {
                event("HEAL +${fmt(diff)} (health ${fmt(lastHealth!!)} -> ${fmt(health)})")
            }
        }

        if (!hurtEvent && hurt > lastHurt && lastHurt == 0.0) {
            event("HURT animation without health loss (blocked / 0 damage hit)")
            hurtEvent = true
        }

        if (hurtEvent) watchKnockback = 3

        if (absorption != null && lastAbsorption != null && absorption != lastAbsorption) {
            event("ABSORPTION ${fmt(lastAbsorption!!)} -> ${fmt(absorption)}")
        }

        if (food != null && lastFood != null && food != lastFood) {
            event("FOOD ${fmt(lastFood!!)} -> ${fmt(food)}")
        }

        if (vel != null && lastVel != null) {
            val dx = vel[0] - lastVel!![0]
            val dy = vel[1] - lastVel!![1]
            val dz = vel[2] - lastVel!![2]
            val horizontal = hypot(dx, dz)

            if (watchKnockback > 0 && (horizontal > 0.15 || dy > 0.2)) {
                event(
                    "KNOCKBACK delta=(${fmt(dx)}, ${fmt(dy)}, ${fmt(dz)}) " +
                            "velocity=(${fmt(vel[0])}, ${fmt(vel[1])}, ${fmt(vel[2])}) " +
                            "horizontal=${fmt(horizontal)}"
                )
                watchKnockback = 0
            } else if (horizontal > 0.6 || dy > 0.8) {
                event(
                    "VELOCITY JUMP delta=(${fmt(dx)}, ${fmt(dy)}, ${fmt(dz)}) " +
                            "(explosion / launch / knockback without hurt?)"
                )
            }
        }
        if (watchKnockback > 0) watchKnockback--

        if (lastPos != null) {
            val moved = sqrt(
                (pos[0] - lastPos!![0]) * (pos[0] - lastPos!![0]) + (pos[2] - lastPos!![2]) * (pos[2] - lastPos!![2])
            )
            if (moved > 8.0) {
                event(
                    "TELEPORT (${fmt(lastPos!![0])}, ${fmt(lastPos!![1])}, ${fmt(lastPos!![2])}) -> " +
                            "(${fmt(pos[0])}, ${fmt(pos[1])}, ${fmt(pos[2])})"
                )
            }
        }

        val onGround = (Refl.call(player, "onGround", "isOnGround") as? Boolean)
        if (onGround != null) {
            if (lastOnGround == true && !onGround && vel != null && vel[1] > 0.3) event("JUMP")
            lastOnGround = onGround
        }

        val sprint = Refl.call(player, "isSprinting") as? Boolean ?: false
        if (sprint != lastSprint) event(if (sprint) "SPRINT started" else "SPRINT stopped")
        lastSprint = sprint

        val sneak = Refl.call(player, "isCrouching", "isShiftKeyDown") as? Boolean ?: false
        if (sneak != lastSneak) event(if (sneak) "SNEAK started" else "SNEAK stopped")
        lastSneak = sneak

        val fire = Refl.call(player, "isOnFire") as? Boolean ?: false
        if (fire != lastFire) event(if (fire) "ON FIRE" else "FIRE stopped")
        lastFire = fire

        val stack = Refl.call(player, "getMainHandItem")
        val item = if (stack == null || Refl.call(stack, "isEmpty") == true) "empty" else
            (Refl.text(Refl.call(stack, "getHoverName")) ?: stack.toString()) +
                    " x${(Refl.call(stack, "getCount") as? Number)?.toInt() ?: 1}"
        if (lastItem != null && item != lastItem) event("HELD ITEM $lastItem -> $item")
        lastItem = item

        if (health != null && health <= 0.0 && !dead) {
            dead = true
            event("DEAD at (${fmt(pos[0])}, ${fmt(pos[1])}, ${fmt(pos[2])})")
        } else if (health != null && health > 0.0) {
            dead = false
        }

        lastHealth = health
        lastAbsorption = absorption
        lastFood = food
        lastHurt = hurt
        lastVel = vel
        lastPos = pos
    }
}
