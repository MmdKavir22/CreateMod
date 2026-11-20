package com.sushitriggers.ctjs.triggers

import com.sushitriggers.ctjs.engine.ILoader

class RegularTrigger(method: Any, triggerType: TriggerType, loader: ILoader) :
    Trigger(method, triggerType, loader) {
    override fun trigger(args: Array<out Any?>) {
        callMethod(args)
    }
}
