// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.task

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import org.gradle.util.GradleVersion
import org.jetbrains.plugins.gradle.importing.GradleImportingTestCase
import org.jetbrains.plugins.gradle.importing.TestGradleBuildScriptBuilder
import org.jetbrains.plugins.gradle.importing.TestGradleBuildScriptBuilder.Companion.buildscript
import org.jetbrains.plugins.gradle.settings.DistributionType
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings
import org.jetbrains.plugins.gradle.tooling.builder.AbstractModelBuilderTest
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.junit.Test

class GradleTaskManagerTest: UsefulTestCase() {
  private lateinit var myTestFixture: IdeaProjectTestFixture
  private lateinit var myProject: Project
  private lateinit var tm: GradleTaskManager
  private lateinit var taskId: ExternalSystemTaskId
  private lateinit var gradleExecSettings: GradleExecutionSettings


  override fun setUp() {
    super.setUp()
    myTestFixture = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder(name).fixture
    myTestFixture.setUp()
    myProject = myTestFixture.project

    tm = GradleTaskManager()
    taskId = ExternalSystemTaskId.create(GradleConstants.SYSTEM_ID,
                                             ExternalSystemTaskType.EXECUTE_TASK,
                                             myProject)
    gradleExecSettings = GradleExecutionSettings(null, null,
                                                     DistributionType.WRAPPED, false)
  }

  override fun tearDown() {
    try {
      myTestFixture.tearDown()
    } finally {
      super.tearDown()
    }
  }

  @Test
  fun `test task manager uses wrapper task when configured`() {
    val output = runHelpTask(GradleVersion.version("4.8.1"))
    assertTrue("Gradle 4.8.1 should be started", output.anyLineContains("Welcome to Gradle 4.8.1"))
  }

  @Test
  fun `test gradle-version-specific init scripts executed`() {

    val oldMessage = "this should be executed for gradle 3.0"
    val oldVer = VersionSpecificInitScript("""println('$oldMessage')""") { v ->
      v == GradleVersion.version("3.0")
    }
    val intervalMessage = "this should be executed for gradle between 4 and 6"
    val intervalVer = VersionSpecificInitScript("println('$intervalMessage')") { v ->
      v > GradleVersion.version("4.0") && v <= GradleVersion.version("6.0")
    }
    val newerVerMessage = "this should be executed for gradle 4.8 and newer"
    val newerVer = VersionSpecificInitScript("println('$newerVerMessage')") { v ->
      v >= GradleVersion.version("4.8")
    }

    val initScripts = listOf(oldVer, intervalVer, newerVer)
    gradleExecSettings.putUserData(GradleTaskManager.VERSION_SPECIFIC_SCRIPTS_KEY, initScripts)

    val output = runHelpTask(GradleVersion.version("4.9"))

    assertFalse(output.anyLineContains(oldMessage))
    assertTrue(output.anyLineContains(intervalMessage))
    assertTrue(output.anyLineContains(newerVerMessage))
  }

  @Test
  fun `test task manager uses wrapper task when wrapper already exists`() {
    runWriteAction {
      val baseDir = PlatformTestUtil.getOrCreateProjectBaseDir(myProject)
      val wrapperProps = baseDir
        .createChildDirectory(this, "gradle")
        .createChildDirectory(this, "wrapper")
        .createChildData(this, "gradle-wrapper.properties")

      VfsUtil.saveText(wrapperProps, """
      distributionBase=GRADLE_USER_HOME
      distributionPath=wrapper/dists
      distributionUrl=${AbstractModelBuilderTest.DistributionLocator().getDistributionFor(GradleVersion.version("4.8"))}
      zipStoreBase=GRADLE_USER_HOME
      zipStorePath=wrapper/dists
    """.trimIndent())
    }

    val output = runHelpTask(GradleVersion.version("4.9"))

    assertTrue("Gradle 4.9 should execute 'help' task", output.anyLineContains("Welcome to Gradle 4.9"))
    assertFalse("Gradle 4.8 should not execute 'help' task", output.anyLineContains("Welcome to Gradle 4.8"))
  }


  private fun runHelpTask(gradleVersion: GradleVersion): TaskExecutionOutput {
    createBuildFile(gradleVersion) {
      withPrefix {
        call("wrapper") {
          assign("gradleVersion", gradleVersion.version)
        }
      }
    }

    gradleExecSettings.javaHome = GradleImportingTestCase.requireJdkHome(gradleVersion)

    val listener = TaskExecutionOutput()
    tm.executeTasks(
      taskId,
      listOf("help"),
      myProject.basePath!!,
      gradleExecSettings,
      null,
      listener
    )
    return listener
  }

  private fun createBuildFile(gradleVersion: GradleVersion, configure: TestGradleBuildScriptBuilder.() -> Unit) {
    runWriteAction {
      val baseFile = PlatformTestUtil.getOrCreateProjectBaseDir(myProject)
      val buildScriptFile = baseFile.createChildData(null, "build.gradle")
      VfsUtil.saveText(buildScriptFile, buildscript(gradleVersion, configure))
    }
  }
}

class TaskExecutionOutput: ExternalSystemTaskNotificationListenerAdapter() {
  private val storage = mutableListOf<String>()
  override fun onTaskOutput(id: ExternalSystemTaskId, text: String, stdOut: Boolean) {
    storage.add(text)
  }

  fun anyLineContains(something: String): Boolean = storage.any { it.contains(something) }
}