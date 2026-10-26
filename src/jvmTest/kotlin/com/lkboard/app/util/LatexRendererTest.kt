package com.lkboard.app.util

import com.lkboard.app.model.BoardElement
import com.lkboard.app.ui.viewmodel.BoardViewModel
import kotlin.test.Test
import kotlin.test.assertNotNull

class LatexRendererTest {

    @Test
    fun testAddFormulaInViewModel() {
        val vm = BoardViewModel()
        val formula = "\\int_{0}^{\\infty} e^{-x^2} dx = \\frac{\\sqrt{\\pi}}{2}"
        vm.openLatexDialog()
        vm.saveLatexFormula(formula)

        val elements = vm.state.value.elements
        println("Elements count: ${elements.size}")
        val formulaElement = elements.values.filterIsInstance<BoardElement.LatexFormula>().firstOrNull()
        assertNotNull(formulaElement)
        println("Formula element: $formulaElement")
    }
}
