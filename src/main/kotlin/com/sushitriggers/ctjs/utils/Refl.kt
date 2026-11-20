package com.sushitriggers.ctjs.utils

object Refl {
    fun call(target: Any?, vararg names: String): Any? {
        if (target == null) return null

        for (name in names) {
            val method = target.javaClass.methods.firstOrNull { it.name == name && it.parameterCount == 0 } ?: continue
            try {
                return method.invoke(target)
            } catch (e: Throwable) {
            }
        }

        return null
    }

    fun callArg(target: Any?, name: String, arg: Any): Any? {
        if (target == null) return null

        val method = target.javaClass.methods.firstOrNull { it.name == name && it.parameterCount == 1 } ?: return null
        return try {
            method.invoke(target, arg)
        } catch (e: Throwable) {
            null
        }
    }

    fun field(target: Any?, name: String): Any? {
        if (target == null) return null

        return try {
            target.javaClass.getField(name).get(target)
        } catch (e: Throwable) {
            null
        }
    }

    fun get(target: Any?, name: String): Any? {
        val getter = "get" + name.replaceFirstChar(Char::uppercase)
        return call(target, name, getter) ?: field(target, name)
    }

    fun num(target: Any?, name: String): Double? {
        val getter = "get" + name.replaceFirstChar(Char::uppercase)
        val value = call(target, name, getter) ?: field(target, name)
        return (value as? Number)?.toDouble()
    }

    fun text(component: Any?): String? {
        return call(component, "getString")?.toString()
    }
}
