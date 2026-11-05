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
        PolinoDatabase.MIGRATION_5_6,
        PolinoDatabase.MIGRATION_6_7,
        PolinoDatabase.MIGRATION_7_8,
        PolinoDatabase.MIGRATION_8_9,
        PolinoDatabase.MIGRATION_9_10,
        PolinoDatabase.MIGRATION_10_11,
        PolinoDatabase.MIGRATION_11_12,
        PolinoDatabase.MIGRATION_12_13,
        PolinoDatabase.MIGRATION_13_14,
        PolinoDatabase.MIGRATION_14_15,
        PolinoDatabase.MIGRATION_15_16,
        PolinoDatabase.MIGRATION_16_17,
        PolinoDatabase.MIGRATION_17_18,
        PolinoDatabase.MIGRATION_18_19,
        PolinoDatabase.MIGRATION_19_20,
        PolinoDatabase.MIGRATION_20_21,
        PolinoDatabase.MIGRATION_21_22,
        PolinoDatabase.MIGRATION_22_23
    ).build()
    private val dao = database.transactionDao()
    private val accountDao = database.accountDao()
    private val categoryDao = database.categoryDao()
    private val recurringDao = database.recurringDao()
    private val goalDao = database.goalDao()
    private val installmentDao = database.installmentDao()
    private val debtDao = database.debtDao()
    private val taskDao = database.taskDao()
    private val eventDao = database.eventDao()
    private val assetDao = database.assetDao()
    private val smsPatternDao = database.smsPatternDao()
    private val chequeReminderDao = database.chequeReminderDao()
    private val accountSmsSenderDao = database.accountSmsSenderDao()
    private val planDao = database.planDao()
    private val noteDao = database.noteDao()
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

    suspend fun addAccount(name: String, bankName: String? = null): AccountEntity {
        val account = AccountEntity(
            UUID.randomUUID().toString(),
            name,
            bankName?.ifBlank { null }
        )
        accountDao.upsert(account)
        return account
    }

    /**
     * تغییر نام و نام بانک حساب؛ شناسه حساب ثابت می‌ماند پس همه
     * تراکنش‌ها، بودجه‌ها و اهدافش سر جایشان می‌مانند. نام تکراری یا
     * خالی رد می‌شود؛ تغییر نام به همان نام فعلی موفقِ بی‌اثر است.
     * نام بانک خالی یعنی بدون بانک.
     */
    suspend fun renameAccount(id: String, newName: String, newBankName: String?): Boolean {
        if (newName.isBlank()) return false
        val all = accountDao.getAll()
        val current = all.firstOrNull { it.id == id } ?: return false
        if (current.name == newName && current.bankName == newBankName) return true
        if (all.any { it.id != id && it.name == newName }) return false
        accountDao.upsert(current.copy(name = newName, bankName = newBankName))
        return true
    }

    /**
     * حذف حساب؛ تراکنش‌ها، هدف‌ها و قاعده‌های تکرارشونده‌اش به «اصلی»
     * منتقل می‌شوند و هیچ تراکنشی از دست نمی‌رود. حساب «اصلی» خودش
     * حذف نمی‌شود.
     */
    suspend fun deleteAccount(id: String): Boolean {
        if (id == "main") return false
        accountDao.reassignTransactions(id, "main")
        goalDao.reassignAccounts(id, "main")
        recurringDao.reassignAccounts(id, "main")
        accountSmsSenderDao.deleteForAccount(id)
        accountDao.deleteById(id)
        return true
    }

    /** همه اتصال‌های سرشماره پیامک به حساب‌ها (مرتب بر اساس حساب و فرستنده). */
    suspend fun getAccountSmsLinks(): List<AccountSmsSenderEntity> =
        accountSmsSenderDao.getAll()

    /** سرشماره‌های متصل به یک حساب. */
    suspend fun getAccountSmsSenders(accountId: String): List<AccountSmsSenderEntity> =
        accountSmsSenderDao.getForAccount(accountId)

    /** اتصال یک سرشماره به یک حساب؛ سرشماره خالی رد می‌شود. */
    suspend fun addAccountSmsSender(accountId: String, sender: String): Boolean {
        val trimmed = sender.trim()
        if (trimmed.isEmpty()) return false
        if (accountDao.getAll().none { it.id == accountId }) return false
        accountSmsSenderDao.upsert(AccountSmsSenderEntity(accountId, trimmed))
        return true
    }

    /** قطع اتصال یک سرشماره از یک حساب. */
    suspend fun removeAccountSmsSender(accountId: String, sender: String) {
        accountSmsSenderDao.deleteByAccountAndSender(accountId, sender)
    }

    /** درج یا جایگزینی اتصال سرشماره (برای بازیابی پشتیبان). */
    suspend fun upsertAccountSmsSender(link: AccountSmsSenderEntity) {
        if (link.sender.isBlank()) return
        accountSmsSenderDao.upsert(link)
    }

    /** دسته‌ها؛ اگر جدول خالی باشد دسته‌های پیش‌فرض ساخته می‌شوند. */
    suspend fun getCategories(): List<CategoryEntity> {
        val existing = categoryDao.getAll()
        if (existing.isNotEmpty()) return existing
        DEFAULT_CATEGORIES.forEach { categoryDao.upsert(it) }
        return categoryDao.getAll()
    }

    /**
     * افزودن دسته؛ نام تکراری رد می‌شود. اگر parentId داده شود، والد
     * باید دسته اصلیِ موجود باشد و نوع زیردسته از والد به ارث می‌رسد؛
     * زیردسته نمی‌تواند خودش والد شود.
     */
    suspend fun addCategory(
        name: String,
        isIncome: Boolean,
        isInvestment: Boolean = false,
        parentId: String? = null
    ): Boolean {
        val all = categoryDao.getAll()
        if (all.any { it.name == name }) return false
        if (parentId != null) {
            val parent = all.firstOrNull { it.name == parentId } ?: return false
            if (parent.parentId != null) return false
            categoryDao.upsert(
                CategoryEntity(name, parent.isIncome, parent.isInvestment, parentId)
            )
        } else {
            categoryDao.upsert(CategoryEntity(name, isIncome, isInvestment, null))
        }
        return true
    }

    /**
     * حذف دسته؛ تراکنش‌هایش به «سایر» منتقل و بودجه‌هایش حذف می‌شوند.
     * «سایر» خودش حذف نمی‌شود. دسته اصلی‌ای که هنوز زیردسته دارد
     * حذف نمی‌شود تا زیردسته‌هایش اول حذف شوند.
     */
    suspend fun deleteCategory(name: String): Boolean {
        if (!canDeleteCategory(name)) return false
        if (categoryDao.getAll().any { it.parentId == name }) return false
        dao.reassignCategory(name, FALLBACK_CATEGORY)
        recurringDao.renameCategory(name, FALLBACK_CATEGORY)
        budgetDao.deleteForCategory(name)
        categoryDao.deleteByName(name)
        return true
    }

    /**
     * تغییر نام دسته؛ تراکنش‌ها، بودجه‌ها و قاعده‌های تکرارشونده‌اش با
     * نام جدید ادامه می‌دهند و نوع دسته (هزینه/درآمد/سرمایه‌گذاری)
     * حفظ می‌شود. اگر دسته اصلی باشد، زیردسته‌هایش هم به نام جدید
     * وصل می‌شوند. «سایر» قابل تغییرنام نیست چون نقطه اتکای حذف
     * دسته‌هاست؛ نام تکراری یا خالی رد می‌شود.
     */
    suspend fun renameCategory(oldName: String, newName: String): Boolean {
        if (newName.isBlank() || oldName == FALLBACK_CATEGORY) return false
        if (oldName == newName) return true
        val all = categoryDao.getAll()
        val current = all.firstOrNull { it.name == oldName } ?: return false
        if (all.any { it.name == newName }) return false
        categoryDao.upsert(
            CategoryEntity(
                newName,
                current.isIncome,
                current.isInvestment,
                current.parentId,
                current.iconKey,
                current.colorArgb
            )
        )
        dao.reassignCategory(oldName, newName)
        budgetDao.renameCategory(oldName, newName)
        recurringDao.renameCategory(oldName, newName)
        categoryDao.reassignParent(oldName, newName)
        categoryDao.deleteByName(oldName)
        return true
    }

    /**
     * تغییر سبک دسته (آیکون و رنگ شاخص)؛ نام، نوع و والد دسته دست
     * نمی‌خورند. کلید آیکون ناشناخته یا رنگ صفر به معنی «بدون سبک
     * شخصی» است و رفتار قبلی برمی‌گردد.
     */
    suspend fun updateCategoryStyle(
        name: String,
        iconKey: String,
        colorArgb: Long
    ): Boolean {
        val current = categoryDao.getAll().firstOrNull { it.name == name } ?: return false
        categoryDao.upsert(current.copy(iconKey = iconKey, colorArgb = colorArgb))
        return true
    }

    /**
     * تغییر نوع و والد دسته؛ نام و سبک دست نمی‌خورند و پرچم‌های نوع
     * همیشه از یک نوع واحد ساخته می‌شوند تا از هم جدا نیفتند. چون
     * تراکنش‌ها با نام دسته ذخیره می‌شوند، تغییر نوع/والد در گزارش‌ها
     * و فهرست‌ها برای تراکنش‌های قبلی هم اعمال می‌شود؛ واسط کاربری
     * پیش از ذخیره، شمار تراکنش‌های درگیر را در دیالوگ تأیید نشان
     * می‌دهد. والد نامعتبر (خود دسته، نواده، غیراصلی، ناهم‌نوع، یا
     * وقتی دسته زیردسته دارد) رد می‌شود و «سایر» نوع/والدش عوض نمی‌شود.
     */
    suspend fun updateCategoryTypeAndParent(
        name: String,
        type: CategoryType,
        parentId: String?
    ): Boolean {
        if (name == FALLBACK_CATEGORY) return false
        val all = categoryDao.getAll()
        val current = all.firstOrNull { it.name == name } ?: return false
        if (!isValidParent(all, name, type, parentId)) return false
        val (isIncome, isInvestment) = typeFlags(type)
        categoryDao.upsert(
            current.copy(isIncome = isIncome, isInvestment = isInvestment, parentId = parentId)
        )
        return true
    }

    suspend fun getRecurring(): List<RecurringEntity> = recurringDao.getAll()

    suspend fun addRecurring(item: RecurringEntity) = recurringDao.upsert(item)

    suspend fun updateRecurring(item: RecurringEntity) = recurringDao.upsert(item)

    suspend fun deleteRecurring(id: String) = recurringDao.deleteById(id)

    suspend fun getGoals(): List<GoalEntity> = goalDao.getAll()

    suspend fun addGoal(item: GoalEntity) = goalDao.upsert(item)

    suspend fun deleteGoal(id: String) = goalDao.deleteById(id)

    suspend fun getInstallments(): List<InstallmentEntity> = installmentDao.getAll()

    suspend fun addInstallment(item: InstallmentEntity) = installmentDao.upsert(item)

    suspend fun deleteInstallment(id: String) = installmentDao.deleteById(id)

    suspend fun getDebts(): List<DebtEntity> = debtDao.getAll()

    suspend fun addDebt(item: DebtEntity) = debtDao.upsert(item)

    suspend fun deleteDebt(id: String) = debtDao.deleteById(id)

    suspend fun getTasks(): List<TaskEntity> = taskDao.getAll()

    suspend fun addTask(item: TaskEntity) = taskDao.upsert(item)

    suspend fun deleteTask(id: String) = taskDao.deleteById(id)

    suspend fun toggleTaskDone(item: TaskEntity) =
        taskDao.upsert(item.copy(done = !item.done))

    suspend fun getPlans(): List<PlanEntity> = planDao.getAll()

    suspend fun addPlan(item: PlanEntity) = planDao.upsert(item)

    suspend fun deletePlan(id: String) = planDao.deleteById(id)

    suspend fun togglePlanDone(item: PlanEntity) =
        planDao.upsert(item.copy(done = !item.done))

    suspend fun getNotes(): List<NoteEntity> = noteDao.getAll()

    suspend fun addNote(item: NoteEntity) = noteDao.upsert(item)

    suspend fun deleteNote(id: String) = noteDao.deleteById(id)

    suspend fun getCheques(): List<ChequeReminderEntity> = chequeReminderDao.getAll()

    suspend fun addCheque(item: ChequeReminderEntity) = chequeReminderDao.upsert(item)

    suspend fun deleteCheque(id: String) = chequeReminderDao.deleteById(id)

    suspend fun settleCheque(item: ChequeReminderEntity) {
        if (item.settled) return
        chequeReminderDao.upsert(item.copy(settled = true))
    }

    suspend fun getEvents(): List<EventEntity> = eventDao.getAll()

    suspend fun addEvent(item: EventEntity) = eventDao.upsert(item)

    suspend fun deleteEvent(id: String) = eventDao.deleteById(id)

    suspend fun getAssets(): List<AssetEntity> = assetDao.getAll()

    suspend fun addAsset(item: AssetEntity) = assetDao.upsert(item)

    suspend fun deleteAsset(id: String) = assetDao.deleteById(id)

    suspend fun getPatterns(): List<SmsPatternEntity> = smsPatternDao.getAll()

    suspend fun addPattern(item: SmsPatternEntity) = smsPatternDao.upsert(item)

    suspend fun deletePattern(id: String) = smsPatternDao.deleteById(id)

    /**
     * تسویه طلب یا بدهی: رکورد تسویه‌شده می‌شود و یک تراکنش واقعی ثبت
     * می‌شود — درآمد برای طلب، هزینه برای بدهی.
     */
    suspend fun settleDebt(item: DebtEntity) {
        if (item.settled) return
        debtDao.upsert(item.copy(settled = true))
        dao.upsert(
            TransactionEntity(
                id = UUID.randomUUID().toString(),
                title = "تسویه: ${item.personName}",
                amount = item.amount,
                income = item.isReceivable,
                category = "سایر",
                dateMillis = System.currentTimeMillis(),
                accountId = item.accountId,
                note = ""
            )
        )
    }

    /**
     * پرداخت یک قسط: شمارنده پرداخت یکی زیاد می‌شود و یک تراکنش هزینه
     * به مبلغ ذخیره‌شده همان قسط در حساب قسط ثبت می‌شود.
     */
    suspend fun payInstallment(item: InstallmentEntity) {
        if (item.paidCount >= item.monthCount) return
        installmentDao.upsert(item.copy(paidCount = item.paidCount + 1))
        dao.upsert(
            TransactionEntity(
                id = UUID.randomUUID().toString(),
                title = "قسط: ${item.title}",
                amount = item.installmentAmount,
                income = false,
                category = "سایر",
                dateMillis = System.currentTimeMillis(),
                accountId = item.accountId,
                note = ""
            )
        )
    }

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
