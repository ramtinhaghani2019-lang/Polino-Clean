package com.polino.app.data

import android.content.Context
import androidx.room.Room

internal class BudgetRepository(context: Context) {
    private val dao = Room.databaseBuilder(
        context.applicationContext, BudgetDatabase::class.java, "polino_budgets_db"
    ).build().budgetDao()

    suspend fun getBudgets(yearMonth: String): List<BudgetEntity> =
        dao.getForMonth(yearMonth)

    suspend fun setBudget(yearMonth: String, category: String, limitAmount: Long) {
        dao.upsert(BudgetEntity(yearMonth, category, limitAmount))
    }

    suspend fun deleteBudget(yearMonth: String, category: String) {
        dao.delete(yearMonth, category)
    }
}
