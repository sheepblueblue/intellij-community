// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.idea.base.projectStructure.languageVersionSettings
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.setType
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.namedFunctionVisitor
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class MainFunctionReturnUnitInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return namedFunctionVisitor(fun(function: KtNamedFunction) {
            val isMain = function.name == "main"
            val testAnnotations = function.annotationEntries.filter { "Test" in it.text }
            if (!isMain && testAnnotations.isEmpty()) return

            val descriptor = function.descriptor as? FunctionDescriptor ?: return
            if (isMain) {
                val mainFunctionDetector =
                    MainFunctionDetector(function.languageVersionSettings) { it.resolveToDescriptorIfAny() }
                if (!mainFunctionDetector.isMain(descriptor, checkReturnType = false)) return
            } else {
                val junitTestFqNames = listOf(FqName("org.junit.Test"), FqName("org.junit.jupiter.api.Test"))
                if (testAnnotations.none { it.fqName() in junitTestFqNames }) return
            }
            if (descriptor.returnType?.let { KotlinBuiltIns.isUnit(it) } == true) return

            holder.registerProblem(
                function.nameIdentifier ?: function,
                KotlinBundle.message("0.should.return.unit", if (isMain) "main" else KotlinBundle.message("junit.test")),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                ChangeMainFunctionReturnTypeToUnitFix(function.typeReference != null)
            )
        })
    }
}

private fun KtAnnotationEntry.fqName(): FqName? {
    val typeReference = this.typeReference ?: return null
    val context = typeReference.analyze(BodyResolveMode.PARTIAL)
    return context[BindingContext.TYPE, typeReference]?.constructor?.declarationDescriptor?.fqNameSafe
}

private class ChangeMainFunctionReturnTypeToUnitFix(private val hasExplicitReturnType: Boolean) : LocalQuickFix {
    override fun getName() = if (hasExplicitReturnType)
        KotlinBundle.message("change.main.function.return.type.to.unit.fix.text2")
    else
        KotlinBundle.message("change.main.function.return.type.to.unit.fix.text")

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val function = descriptor.psiElement.getNonStrictParentOfType<KtNamedFunction>() ?: return
        if (function.hasBlockBody()) {
            function.typeReference = null
        } else {
            function.setType(StandardNames.FqNames.unit.asString())
        }
    }
}
