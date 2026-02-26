package com.alextdev.mermaidvisualizer

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey

private const val BUNDLE = "messages.MyMessageBundle"

internal object MyMessageBundle : DynamicBundle(BUNDLE) {

    fun message(key: @PropertyKey(resourceBundle = BUNDLE) String, vararg params: Any?): @Nls String {
        return getMessage(key, *params)
    }
}
