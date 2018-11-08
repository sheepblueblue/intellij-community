/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.j2k.tree.impl

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.j2k.ConversionContext
import org.jetbrains.kotlin.j2k.JKSymbolProvider
import org.jetbrains.kotlin.j2k.ast.Mutability
import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.j2k.conversions.resolveFqName
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.JKLiteralExpression.LiteralType
import org.jetbrains.kotlin.j2k.tree.JKLiteralExpression.LiteralType.BOOLEAN
import org.jetbrains.kotlin.j2k.tree.JKLiteralExpression.LiteralType.NULL
import org.jetbrains.kotlin.j2k.tree.visitors.JKVisitor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction

class JKFileImpl : JKFile, JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitFile(this, data)

    override var declarationList by children<JKDeclaration>()
}

class JKClassImpl(
    modifierList: JKModifierList,
    name: JKNameIdentifier,
    inheritance: JKInheritanceInfo,
    override var classKind: JKClass.ClassKind,
    typeParameterList: JKTypeParameterList
) : JKClass, JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitClass(this, data)

    override var name by child(name)
    override var modifierList by child(modifierList)
    override var declarationList by children<JKDeclaration>()
    override val inheritance by child(inheritance)
    override var typeParameterList: JKTypeParameterList by child(typeParameterList)
}

class JKNameIdentifierImpl(override val value: String) : JKNameIdentifier, JKElementBase(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitNameIdentifier(this, data)
}

class JKModifierListImpl(
    modifiers: List<JKModifier> = emptyList()
) : JKModifierList, JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    constructor(vararg modifiers: JKModifier) : this(modifiers.asList())

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitModifierList(this, data)

    override var modifiers: List<JKModifier> by children(modifiers)
}


var JKModifierList.modality
    get() = modifiers.filterIsInstance<JKModalityModifier>().first().modality
    set(value) {
        modifiers = modifiers.filterNot { it is JKModalityModifier } + JKModalityModifierImpl(value)
    }

var JKModifierList.visibility
    get() = modifiers.filterIsInstance<JKAccessModifier>().first().visibility
    set(value) {
        modifiers = modifiers.filterNot { it is JKAccessModifier } + JKAccessModifierImpl(value)
    }

var JKModifierList.mutability
    get() = modifiers.filterIsInstance<JKMutabilityModifier>().firstOrNull()?.mutability ?: Mutability.Default
    set(value) {
        modifiers = modifiers.filterNot { it is JKMutabilityModifier } +
                listOfNotNull(if (value != Mutability.Default) JKMutabilityModifierImpl(value) else null)
    }

class JKParameterImpl(
    type: JKTypeElement,
    name: JKNameIdentifier,
    modifierList: JKModifierList,
    initializer: JKExpression = JKStubExpressionImpl()

) : JKParameter, JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitParameter(this, data)

    override var modifierList by child(modifierList)
    override var initializer by child(initializer)
    override var name by child(name)
    override var type by child(type)
}

class JKBlockImpl(statements: List<JKStatement> = emptyList()) : JKBlock, JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitBlock(this, data)

    override var statements by children(statements)
}

//todo split to java and kt sbinary expression
class JKBinaryExpressionImpl(
    left: JKExpression,
    right: JKExpression,
    override var operator: JKOperator
) : JKBinaryExpression,
    JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitBinaryExpression(this, data)
    override var right by child(right)
    override var left by child(left)
}



class JKPrefixExpressionImpl(expression: JKExpression, override var operator: JKOperator) : JKPrefixExpression, JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitPrefixExpression(this, data)

    override var expression by child(expression)
}

class JKPostfixExpressionImpl(expression: JKExpression, override var operator: JKOperator) : JKPostfixExpression, JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitPostfixExpression(this, data)

    override var expression by child(expression)
}

class JKExpressionListImpl(expressions: List<JKExpression> = emptyList()) : JKExpressionList, JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    constructor(vararg expresions: JKExpression) : this(expresions.asList())
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitExpressionList(this, data)

    override var expressions by children(expressions)
}

class JKQualifiedExpressionImpl(
    receiver: JKExpression,
    override var operator: JKQualifier,
    selector: JKExpression
) : JKQualifiedExpression, JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitQualifiedExpression(this, data)

    override var receiver: JKExpression by child(receiver)
    override var selector: JKExpression by child(selector)
}

class JKExpressionStatementImpl(expression: JKExpression) : JKExpressionStatement, JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitExpressionStatement(this, data)

    override val expression: JKExpression by child(expression)
}

class JKDeclarationStatementImpl(declaredStatements: List<JKDeclaration>) : JKDeclarationStatement, JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    override val declaredStatements by children(declaredStatements)

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitDeclarationStatement(this, data)
}

class JKArrayAccessExpressionImpl(
    expression: JKExpression,
    indexExpression: JKExpression
) : JKArrayAccessExpression, JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitArrayAccessExpression(this, data)

    override var expression: JKExpression by child(expression)
    override var indexExpression: JKExpression by child(indexExpression)
}

class JKParenthesizedExpressionImpl(expression: JKExpression) : JKParenthesizedExpression, JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitParenthesizedExpression(this, data)

    override var expression: JKExpression by child(expression)
}

class JKTypeCastExpressionImpl(override var expression: JKExpression, type: JKTypeElement) : JKTypeCastExpression, JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitTypeCastExpression(this, data)

    override var type by child(type)
}

class JKTypeElementImpl(override var type: JKType) : JKTypeElement, JKElementBase(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitTypeElement(this, data)
}

class JKClassTypeImpl(
    override val classReference: JKClassSymbol,
    override val parameters: List<JKType> = emptyList(),
    override val nullability: Nullability = Nullability.Default
) : JKClassType

object JKNoTypeImpl: JKNoType {
    override val nullability: Nullability = Nullability.NotNull
}

class JKStarProjectionTypeImpl : JKStarProjectionType

class JKUnresolvedClassType(
    val name: String,
    override val parameters: List<JKType>,
    override val nullability: Nullability = Nullability.Default
) : JKParametrizedType

fun JKType.fqName(): String =
    when (this) {
        is JKClassType -> {
            val target = classReference?.target
            when (target) {
                is KtClass -> target.fqName?.asString() ?: throw RuntimeException("FqName can not be calculated")
                is PsiClass -> target.qualifiedName ?: throw RuntimeException("FqName can not be calculated")
                else -> TODO(target.toString())
            }
        }
        is JKUnresolvedClassType -> name
        is JKJavaPrimitiveType -> jvmPrimitiveType.name
        else -> TODO(toString())
    }

fun JKType.equalsByName(other: JKType) = fqName() == other.fqName()


class JKNullLiteral : JKLiteralExpression, JKElementBase(), PsiOwner by PsiOwnerImpl() {
    override val literal: String
        get() = "null"
    override val type: LiteralType
        get() = NULL

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitLiteralExpression(this, data)
}

class JKBooleanLiteral(val value: Boolean) : JKLiteralExpression, JKElementBase(), PsiOwner by PsiOwnerImpl() {
    override val literal: String
        get() = value.toString()
    override val type: LiteralType
        get() = BOOLEAN

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitLiteralExpression(this, data)
}

fun JKLiteralExpression.LiteralType.toJkType(symbolProvider: JKSymbolProvider): JKType {
    fun defaultTypeByName(name: String) =
        JKClassTypeImpl(
            symbolProvider.provideDirectSymbol(
                resolveFqName(ClassId.fromString("kotlin.$name"), symbolProvider.symbolsByPsi.keys.first())!!
            ) as JKClassSymbol, emptyList(), Nullability.NotNull
        )

    return when (this) {
        JKLiteralExpression.LiteralType.CHAR -> defaultTypeByName("Char")
        JKLiteralExpression.LiteralType.BOOLEAN -> defaultTypeByName("Boolean")
        JKLiteralExpression.LiteralType.INT -> defaultTypeByName("Int")
        JKLiteralExpression.LiteralType.LONG -> defaultTypeByName("Long")
        JKLiteralExpression.LiteralType.FLOAT -> defaultTypeByName("Float")
        JKLiteralExpression.LiteralType.DOUBLE -> defaultTypeByName("Double")
        JKLiteralExpression.LiteralType.NULL ->
            ClassId.topLevel(KotlinBuiltIns.FQ_NAMES.unit.toSafe()).toKtClassType(symbolProvider)
        JKLiteralExpression.LiteralType.STRING ->
            ClassId.topLevel(KotlinBuiltIns.FQ_NAMES.string.toSafe()).toKtClassType(symbolProvider)
    }
}

class JKLocalVariableImpl(modifierList: JKModifierList, type: JKTypeElement, name: JKNameIdentifier, initializer: JKExpression) :
    JKLocalVariable, JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    override var modifierList by child(modifierList)
    override var initializer by child(initializer)
    override var name by child(name)
    override val type by child(type)

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitLocalVariable(this, data)
}

class JKStubExpressionImpl : JKStubExpression, JKElementBase(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitStubExpression(this, data)
}

object JKBodyStub : JKBlock, JKTreeElement {
    override fun copy(): JKTreeElement = this

    override var statements: List<JKStatement>
        get() = emptyList()
        set(value) {}

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitBlock(this, data)

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {}

    override val parent: JKElement?
        get() = null

    override fun detach(from: JKElement) {
    }

    override fun attach(to: JKElement) {
    }
}

class JKBlockStatementImpl(block: JKBlock) : JKBlockStatement, JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    override var block by child(block)

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitBlockStatement(this, data)
}

class JKThisExpressionImpl : JKThisExpression, JKElementBase(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitThisExpression(this, data)
}

class JKSuperExpressionImpl : JKSuperExpression, JKElementBase(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitSuperExpression(this, data)
}

class JKWhileStatementImpl(condition: JKExpression, body: JKStatement) : JKWhileStatement, JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    override var condition by child(condition)
    override var body by child(body)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitWhileStatement(this, data)
}

class JKDoWhileStatementImpl(body: JKStatement, condition: JKExpression) : JKDoWhileStatement, JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    override var condition by child(condition)
    override var body by child(body)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitDoWhileStatement(this, data)
}

class JKBreakStatementImpl : JKBreakStatement, JKElementBase(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitBreakStatement(this, data)
}

class JKBreakWithLabelStatementImpl(override var label: JKNameIdentifier) : JKBreakWithLabelStatement, JKElementBase(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitBreakWithLabelStatement(this, data)
}

class JKIfStatementImpl(condition: JKExpression, thenBranch: JKStatement) : JKIfStatement, JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    override var thenBranch by child(thenBranch)
    override var condition by child(condition)

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitIfStatement(this, data)
}

class JKIfElseStatementImpl(condition: JKExpression, thenBranch: JKStatement, elseBranch: JKStatement) : JKIfElseStatement,
    JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    override var elseBranch by child(elseBranch)
    override var thenBranch by child(thenBranch)
    override var condition by child(condition)

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitIfElseStatement(this, data)
}

class JKIfElseExpressionImpl(condition: JKExpression, thenBranch: JKExpression, elseBranch: JKExpression) : JKIfElseExpression,
    JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    override var elseBranch by child(elseBranch)
    override var thenBranch by child(thenBranch)
    override var condition by child(condition)

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitIfElseExpression(this, data)
}

class JKClassAccessExpressionImpl(override var identifier: JKClassSymbol) : JKClassAccessExpression, JKElementBase(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitClassAccessExpression(this, data)
}

class JKModalityModifierImpl(override val modality: JKModalityModifier.Modality) : JKModalityModifier, JKElementBase(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitModalityModifier(this, data)
}

class JKAccessModifierImpl(override val visibility: JKAccessModifier.Visibility) : JKAccessModifier, JKElementBase(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitAccessModifier(this, data)
}

class JKMutabilityModifierImpl(override val mutability: Mutability) : JKMutabilityModifier, JKElementBase(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitMutabilityModifier(this, data)
}

class JKLambdaExpressionImpl(
    parameters: List<JKParameter> = listOf(
        JKParameterImpl(JKTypeElementImpl(JKJavaVoidType), JKNameIdentifierImpl("it"), JKModifierListImpl())
    ), statement: JKStatement, returnType: JKTypeElement = JKTypeElementImpl(JKContextType)
) : JKLambdaExpression, JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    override var statement by child(statement)
    override val returnType by child(returnType)
    override var parameters by children(parameters)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitLambdaExpression(this, data)
}

class JKInheritanceInfoImpl(implements: List<JKTypeElement>) : JKInheritanceInfo, JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    override val inherit: List<JKTypeElement> by children(implements)

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitInheritanceInfo(this, data)
}

class JKDelegationConstructorCallImpl(
    override val identifier: JKMethodSymbol,
    expression: JKExpression,
    arguments: JKExpressionList
) : JKBranchElementBase(), JKDelegationConstructorCall, PsiOwner by PsiOwnerImpl() {
    override var typeArguments by children(emptyList<JKTypeElement>())
    override val expression: JKExpression by child(expression)
    override val arguments: JKExpressionList by child(arguments)

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitDelegationConstructorCall(this, data)
}

class JKFieldAccessExpressionImpl(override var identifier: JKFieldSymbol) : JKFieldAccessExpression, JKElementBase(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitFieldAccessExpression(this, data)
}
val JKStatement.statements: List<JKStatement>
    get() =
        when (this) {
            is JKBlockStatement -> block.statements
            else -> listOf(this)
        }

class JKLabelEmptyImpl : JKLabelEmpty, JKElementBase(), PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitLabelEmpty(this, data)
}
class JKLabelTextImpl(label: JKNameIdentifier) : JKLabelText, JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    override val label: JKNameIdentifier by child(label)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitLabelText(this, data)
}

class JKContinueStatementImpl(label: JKLabel) : JKContinueStatement, JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    override var label: JKLabel by child(label)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitContinueStatement(this, data)
}

class JKLabeledStatementImpl(statement: JKStatement, labels: List<JKNameIdentifier>) : JKLabeledStatement, JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    override var statement: JKStatement by child(statement)
    override val labels: List<JKNameIdentifier> by children(labels)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitLabeledStatement(this, data)
}

class JKEmptyStatementImpl: JKEmptyStatement, JKElementBase() , PsiOwner by PsiOwnerImpl() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitEmptyStatement(this, data)
}

class PsiOwnerImpl(override var psi: PsiElement? = null) : PsiOwner

val JKElement.psi: PsiElement?
    get() = (this as? PsiOwner)?.psi

fun <Elem : PsiElement>JKElement.psi(): Elem? =
    (this as? PsiOwner)?.psi as? Elem

class JKTypeParameterListImpl(typeParameters: List<JKTypeParameter> = emptyList()) : JKTypeParameterList, JKBranchElementBase() {
    override var typeParameters by children(typeParameters)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitTypeParameterList(this, data)
}

class JKTypeParameterImpl(name: JKNameIdentifier, upperBounds: List<JKTypeElement>) : JKTypeParameter, JKBranchElementBase() {
    override var name: JKNameIdentifier by child(name)
    override var upperBounds: List<JKTypeElement> by children(upperBounds)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitTypeParameter(this, data)
}

class JKVarianceTypeParameterTypeImpl(
    override val variance: JKVarianceTypeParameterType.Variance,
    override val boundType: JKType
) : JKVarianceTypeParameterType

class JKTypeParameterTypeImpl(
    override val name: String,
    override val nullability: Nullability = Nullability.Default
) : JKTypeParameterType

class JKEnumConstantImpl(
    name: JKNameIdentifier,
    arguments: JKExpressionList,
    type: JKTypeElement,
    modifierList: JKModifierList
) : JKEnumConstant, JKBranchElementBase(), PsiOwner by PsiOwnerImpl() {
    override var name: JKNameIdentifier by child(name)
    override val arguments: JKExpressionList by child(arguments)
    override val type: JKTypeElement by child(type)
    override var initializer: JKExpression by child(JKStubExpressionImpl())
    override var modifierList: JKModifierList by child(modifierList)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitEnumConstant(this, data)
}

fun JKTypeElement.present(): Boolean =
    type != JKNoTypeImpl