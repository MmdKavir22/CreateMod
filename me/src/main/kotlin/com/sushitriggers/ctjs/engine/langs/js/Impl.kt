package com.sushitriggers.ctjs.engine.langs.js

import com.sushitriggers.ctjs.engine.ILoader
import com.sushitriggers.ctjs.engine.IRegister

object JSRegister : IRegister {
    override fun getImplementationLoader(): ILoader = JSLoader
}
