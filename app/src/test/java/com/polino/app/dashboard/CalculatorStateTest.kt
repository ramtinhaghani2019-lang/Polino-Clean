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
}
