package com.polino.app.data

import android.content.Context
import androidx.room.Room
import java.util.UUID

internal class TransactionRepository(context: Context) {
    private val appContext = context.applicationContext
    private val database = Room.databaseBuilder(
        appContext, PolinoDatabase::class.java, "polino_db"
    ).addMigrations(
        PolinoDatabase.MIGRATION_1_2,
        PolinoDatabase.MIGRATION_2_3,
        PolinoDatabase.MIGRATION_3_4,
        PolinoDatabase.MIGRATION_4_5,
        PolinoDatabase.MIGRATION_5_6
    ).build()
    private val dao = database.transactionDao()
    private val accountDao = database.accountDao()
    private val categoryDao = database.categoryDao()
    private val recurringDao = database.recurringDao()
    private val goalDao = database.goalDao()
    private val budgetDao = Room.databaseBuilder(
        appContext, BudgetDatabase::class.java, "polino_budgets_db"
    ).build().budgetDao()
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    suspend fun getAll(): List<MoneyTransaction> =
        dao.getAll().map { it.toDomain() }

    suspend fun upsert(transaction: MoneyTransaction) {
        dao.upsert(transaction.toEntity())
    }

    suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    /** حساب‌ها؛ اگر جدول خالی باشد حساب «اصلی» ساخته می‌شود. */
    suspend fun getAccounts(): List<AccountEntity> {
        val existing = accountDao.getAll()
        if (existing.isNotEmpty()) return existing
        val main = AccountEntity("main", "اصلی")
        accountDao.upsert(main)
        return listOf(main)
    }

    /** درج یا جایگزینی حساب با شناسه مشخص (برای بازیابی پشتیبان). */
    suspend fun upsertAccount(account: AccountEntity) = accountDao.upsert(account)

    suspend fun addAccount(name: String): AccountEntity {
        val account = AccountEntity(UUID.randomUUID().toString(), name)
        accountDao.upsert(account)
        return account
    }

    /**
     * حذف حساب؛ تراکنش‌هایش به «اصلی» منتقل می‌شوند و هیچ تراکنشی از دست
     * نمی‌رود. حساب «اصلی» خودش حذف نمی‌شود.
     */
    suspend fun deleteAccount(id: String): Boolean {
        if (id == "main") return false
        accountDao.reassignTransactions(id, "main")
        accountDao.deleteById(id)
        return true
    }

    /** دسته‌ها؛ اگر جدول خالی باشد دسته‌های پیش‌فرض ساخته می‌شوند. */
    suspend fun getCategories(): List<CategoryEntity> {
        val existing = categoryDao.getAll()
        if (existing.isNotEmpty()) return existing
        DEFAULT_CATEGORIES.forEach { categoryDao.upsert(it) }
        return categoryDao.getAll()
    }

    /** افزودن دسته؛ نام تکراری رد می‌شود. */
    suspend fun addCategory(name: String, isIncome: Boolean): Boolean {
        if (categoryDao.getAll().any { it.name == name }) return false
        categoryDao.upsert(CategoryEntity(name, isIncome))
        return true
    }

    /**
     * حذف دسته؛ تراکنش‌هایش به «سایر» منتقل و بودجه‌هایش حذف می‌شوند.
     * «سایر» خودش حذف نمی‌شود.
     */
    suspend fun deleteCategory(name: String): Boolean {
        if (!canDeleteCategory(name)) return false
        dao.reassignCategory(name, FALLBACK_CATEGORY)
        budgetDao.deleteForCategory(name)
        categoryDao.deleteByName(name)
        return true
    }

    suspend fun getRecurring(): List<RecurringEntity> = recurringDao.getAll()

    suspend fun addRecurring(item: RecurringEntity) = recurringDao.upsert(item)

    suspend fun updateRecurring(item: RecurringEntity) = recurringDao.upsert(item)

    suspend fun deleteRecurring(id: String) = recurringDao.deleteById(id)

    suspend fun getGoals(): List<GoalEntity> = goalDao.getAll()

    suspend fun addGoal(item: GoalEntity) = goalDao.upsert(item)

    suspend fun deleteGoal(id: String) = goalDao.deleteById(id)

    /**
     * مهاجرت یک‌بارمصرف از JSON قدیمی SharedPreferences به Room.
     * JSON قدیمی دست نمی‌خورد و به‌عنوان پشتیبان باقی می‌ماند؛
     * پرچم مهاجرت از اجرای دوباره جلوگیری می‌کند.
     */
    suspend fun migrateIfNeeded() {
        if (prefs.getBoolean(KEY_MIGRATED, false)) return
        val parsed = parseLegacyTransactions(prefs.getString(KEY, "[]") ?: "[]")
        if (parsed.isNotEmpty()) dao.insertAll(parsed.map { it.toEntity() })
        prefs.edit().putBoolean(KEY_MIGRATED, true).apply()
    }

    private companion object {
        const val PREFS = "polino_finance_v1"
        const val KEY = "transactions"
        const val KEY_MIGRATED = "room_migrated_v1"
    }
}
