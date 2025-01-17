// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.idea.versions

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil
import org.jetbrains.kotlin.idea.configuration.ModuleSourceRootGroup
import org.jetbrains.kotlin.idea.configuration.toModuleGroup

@State(name = "SuppressABINotification")
class SuppressNotificationState : PersistentStateComponent<SuppressNotificationState> {
    var isSuppressed: Boolean = false
    var modulesWithSuppressedNotConfigured = sortedSetOf<String>()

    override fun getState(): SuppressNotificationState = this

    override fun loadState(state: SuppressNotificationState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun getInstance(project: Project): SuppressNotificationState = project.service()

        fun isKotlinNotConfiguredSuppressed(moduleGroup: ModuleSourceRootGroup): Boolean {
            val baseModule = moduleGroup.baseModule
            return baseModule.name in getInstance(baseModule.project).modulesWithSuppressedNotConfigured
        }

        fun suppressKotlinNotConfigured(module: Module) {
            getInstance(module.project).modulesWithSuppressedNotConfigured.add(module.toModuleGroup().baseModule.name)
        }
    }
}
