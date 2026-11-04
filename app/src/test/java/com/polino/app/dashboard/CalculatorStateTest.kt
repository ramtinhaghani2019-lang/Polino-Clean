package com.polino.app.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculatorStateTest {

    @Test
    fun add_works() {
        val calc = CalculatorState()
        calc.inputDigit("1")
        calc.inputDigit("2")
        calc.inputOp('+')
        calc.inputDigit("3")
        calc.inputDigit("4")
        calc.inputEquals()
        assertEquals("46", calc.display)
    }

    @Test
    fun subtract_works() {
        val calc = CalculatorState()
        calc.inputDigit("1")
        calc.inputDigit("0")
        calc.inputOp('−')
        calc.inputDigit("4")
        calc.inputEquals()
        assertEquals("6", calc.display)
    }

    @Test
    fun multiply_works() {
        val calc = CalculatorState()
        calc.inputDigit("6")
        calc.inputOp('×')
        calc.inputDigit("7")
        calc.inputEquals()
        assertEquals("42", calc.display)
    }

    @Test
    fun divide_works() {
        val calc = CalculatorState()
        calc.inputDigit("2")
        calc.inputDigit("0")
        calc.inputOp('÷')
        calc.inputDigit("4")
        calc.inputEquals()
        assertEquals("5", calc.display)
    }

    @Test
    fun divideByZero_keepsValue() {
        val calc = CalculatorState()
        calc.inputDigit("9")
        calc.inputOp('÷')
        calc.inputDigit("0")
        calc.inputEquals()
        assertEquals("9", calc.display)
    }

    @Test
    fun chainedOps_executeImmediately() {
        val calc = CalculatorState()
        calc.inputDigit("5")
        calc.inputOp('+')
        calc.inputDigit("5")
        calc.inputOp('+')
        calc.inputDigit("5")
        calc.inputEquals()
        assertEquals("15", calc.display)
    }

    @Test
    fun clear_resets() {
        val calc = CalculatorState()
        calc.inputDigit("8")
        calc.clear()
        assertEquals("", calc.display)
    }

    @Test
    fun decimalPoint_addsFractionAndIgnoresSecondPoint() {
        val calc = CalculatorState()
        calc.inputDigit("1")
        calc.inputDecimalPoint()
        calc.inputDigit("5")
        calc.inputDecimalPoint()
        calc.inputDigit("2")
        assertEquals("1.52", calc.display)
    }

    @Test
    fun decimalPoint_onFreshStartsWithZero() {
        val calc = CalculatorState()
        calc.inputDecimalPoint()
        calc.inputDigit("5")
        assertEquals("0.5", calc.display)
    }

    @Test
    fun doubleZero_andTripleZero_append() {
        val calc = CalculatorState()
        calc.inputDigit("1")
        calc.inputDigit("00")
        assertEquals("100", calc.display)
        calc.inputDigit("000")
        assertEquals("100000", calc.display)
    }

    @Test
    fun doubleZero_onFreshGivesZero() {
        val calc = CalculatorState()
        calc.inputDigit("00")
        assertEquals("0", calc.display)
    }

    @Test
    fun digitLength_cappedAtTwelve() {
        val calc = CalculatorState()
        repeat(13) { calc.inputDigit("1") }
        assertEquals("111111111111", calc.display)
    }

    @Test
    fun percent_dividesBy100() {
        val calc = CalculatorState()
        calc.inputDigit("2")
        calc.inputDigit("0")
        calc.inputDigit("0")
        calc.inputPercent()
        assertEquals("2", calc.display)
        calc.clear()
        calc.inputDigit("5")
        calc.inputDigit("0")
        calc.inputPercent()
        assertEquals("0.5", calc.display)
    }

    @Test
    fun percent_inChain_usesPercentedValue() {
        val calc = CalculatorState()
        calc.inputDigit("2")
        calc.inputDigit("0")
        calc.inputDigit("0")
        calc.inputOp('+')
        calc.inputDigit("1")
        calc.inputDigit("0")
        calc.inputPercent()
        calc.inputEquals()
        assertEquals("200.1", calc.display)
    }

    @Test
    fun toggleSign_flipsAndRestores() {
        val calc = CalculatorState()
        calc.inputDigit("5")
        calc.inputToggleSign()
        assertEquals("-5", calc.display)
        calc.inputToggleSign()
        assertEquals("5", calc.display)
    }

    @Test
    fun toggleSign_onEmptyAndZero_noop() {
        val calc = CalculatorState()
        calc.inputToggleSign()
        assertEquals("", calc.display)
        calc.inputDigit("0")
        calc.inputToggleSign()
        assertEquals("0", calc.display)
    }

    @Test
    fun backspace_deletesChars() {
        val calc = CalculatorState()
        calc.inputDigit("1")
        calc.inputDigit("2")
        calc.inputDigit("3")
        calc.inputBackspace()
        assertEquals("12", calc.display)
        calc.inputBackspace()
        calc.inputBackspace()
        assertEquals("", calc.display)
    }

    @Test
    fun decimalArithmetic_isExact() {
        val calc = CalculatorState()
        calc.inputDigit("0")
        calc.inputDecimalPoint()
        calc.inputDigit("1")
        calc.inputOp('+')
        calc.inputDigit("0")
        calc.inputDecimalPoint()
        calc.inputDigit("2")
        calc.inputEquals()
        assertEquals("0.3", calc.display)
        calc.clear()
        calc.inputDigit("1")
        calc.inputDigit("0")
        calc.inputOp('÷')
        calc.inputDigit("4")
        calc.inputEquals()
        assertEquals("2.5", calc.display)
    }

    @Test
    fun formatDisplay_emptyAndZero_showZeroToman() {
        assertEquals("۰ تومان", formatCalcDisplay(""))
        assertEquals("۰ تومان", formatCalcDisplay("0"))
    }

    @Test
    fun formatDisplay_decimal_showsPersianFraction() {
        val intPart = java.text.NumberFormat
            .getIntegerInstance(java.util.Locale("fa", "IR")).format(2500L)
        assertEquals("$intPart٫۵ تومان", formatCalcDisplay("2500.5"))
    }
}
