package nl.rubensten.texifyidea.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import nl.rubensten.texifyidea.LatexLanguage
import nl.rubensten.texifyidea.lang.Environment
import nl.rubensten.texifyidea.lang.LatexMode
import nl.rubensten.texifyidea.lang.LatexNoMathCommand
import nl.rubensten.texifyidea.lang.RequiredFileArgument
import nl.rubensten.texifyidea.psi.*
import nl.rubensten.texifyidea.util.inDirectEnvironmentContext

/**
 * @author Sten Wessel, Ruben Schellekens
 */
open class LatexCompletionContributor : CompletionContributor() {

    init {
        // Math mode
        extend(
                CompletionType.BASIC,
                PlatformPatterns.psiElement(LatexTypes.COMMAND_TOKEN)
                        .inside(LatexMathEnvironment::class.java)
                        .withLanguage(LatexLanguage.INSTANCE),
                LatexCommandProvider(LatexMode.MATH)
        )

        // Math mode inside environments
        extend(
                CompletionType.BASIC,
                PlatformPatterns.psiElement(LatexTypes.COMMAND_TOKEN)
                        .with(object : PatternCondition<PsiElement>(null) {
                            override fun accepts(psiElement: PsiElement, processingContext: ProcessingContext): Boolean {
                                return psiElement.inDirectEnvironmentContext(Environment.Context.MATH)
                            }
                        })
                        .withLanguage(LatexLanguage.INSTANCE),
                LatexCommandProvider(LatexMode.MATH)
        )

        // Normal
        extend(
                CompletionType.BASIC,
                PlatformPatterns.psiElement(LatexTypes.COMMAND_TOKEN)
                        .andNot(PlatformPatterns.psiElement()
                                .inside(LatexMathEnvironment::class.java))
                        .with(object : PatternCondition<PsiElement>(null) {
                            override fun accepts(psiElement: PsiElement, processingContext: ProcessingContext): Boolean {
                                return psiElement.inDirectEnvironmentContext(Environment.Context.NORMAL)
                            }
                        })
                        .withLanguage(LatexLanguage.INSTANCE),
                LatexCommandProvider(LatexMode.NORMAL)
        )

        // DefaultEnvironment names
        extend(
                CompletionType.BASIC,
                PlatformPatterns.psiElement()
                        .inside(LatexRequiredParam::class.java)
                        .inside(LatexBeginCommand::class.java)
                        .withLanguage(LatexLanguage.INSTANCE),
                LatexCommandProvider(LatexMode.ENVIRONMENT_NAME)
        )

        // File names
        extend(
                CompletionType.BASIC,
                PlatformPatterns.psiElement().inside(LatexNormalText::class.java)
                        .inside(LatexRequiredParam::class.java)
                        .with(object : PatternCondition<PsiElement>(null) {
                            override fun accepts(psiElement: PsiElement, processingContext: ProcessingContext): Boolean {
                                val command = LatexPsiUtil.getParentOfType(psiElement, LatexCommands::class.java) ?: return false

                                val name = command.commandToken.text
                                val cmd = LatexNoMathCommand.get(name.substring(1)) ?: return false

                                val args = cmd.getArgumentsOf(RequiredFileArgument::class)
                                return !args.isEmpty()
                            }
                        })
                        .withLanguage(LatexLanguage.INSTANCE),
                LatexFileProvider()
        )
    }
}
