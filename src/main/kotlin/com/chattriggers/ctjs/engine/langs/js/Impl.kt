package com.chattriggers.ctjs.engine.langs.js

import com.chattriggers.ctjs.engine.ILoader
import com.chattriggers.ctjs.engine.IRegister

object JSRegister : IRegister {
    override fun getImplementationLoader(): ILoader = JSLoader
}
