package com.shadowinspect.app.data.phone

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────────────
//  ApiQuota – snapshot of one provider's quota state
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Immutable snapshot of a single API provider's quota state.
 *
 * @param providerName  Unique identifier string (e.g. "AbstractAPI").
 * @param totalQuota    Maximum calls allowed per reset period.
 * @param usedQuota     Calls consumed since the last reset.
 * @param resetDate     Epoch-ms timestamp of the next automatic reset.
 * @param isActive      False when the provider has been administratively disabled.
 */
data class ApiQuota(
    val providerName: String,
    val totalQuota: Int,
    val usedQuota: Int,
    val resetDate: Long,
    val isActive: Boolean
) {
    val remainingQuota: Int get() = (totalQuota - usedQuota).coerceAtLeast(0)
    val usagePercent: Float get() = if (totalQuota == 0) 1f else usedQuota.toFloat() / totalQuota
    val isLow: Boolean get() = usagePercent >= LOW_THRESHOLD
    val isExhausted: Boolean get() = remainingQuota == 0

    companion object {
        const val LOW_THRESHOLD = 0.80f    // warn at 80% usage
    }
}

data class QuotaStatus(
    val providerName: String,
    val remainingQuota: Int,
    val percentageUsed: Int,
    val resetInDays: Int,
    val isAvailable: Boolean
)

// ─────────────────────────────────────────────────────────────────────────────
//  Known providers — single source of truth for quotas & priority
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Catalogue of supported free-tier phone validation providers.
 * [priority] drives [ApiUsageTracker.getBestAvailableProvider] — lower = preferred.
 */
enum class PhoneApiProvider(
    val id: String,
    val monthlyQuota: Int,
    val priority: Int,
    val description: String
) {
    ABSTRACT_API(
        id          = "AbstractAPI",
        monthlyQuota = 250,
        priority    = 1,
        description = "abstractapi.com — 250 free requests/month, returns carrier + line type"
    ),
    NUMVERIFY(
        id          = "Numverify",
        monthlyQuota = 250,
        priority    = 2,
        description  = "numverify.com — 250 free requests/month, returns carrier + location"
    ),
    IPQUALITY(
        id          = "IPQualityScore",
        monthlyQuota = 5000,
        priority    = 3,
        description  = "ipqualityscore.com — 5000 free requests/month, returns VoIP/Prepaid data"
    ),
    VERIPHONE(
        id          = "Veriphone",
        monthlyQuota = 100,
        priority    = 4,
        description = "veriphone.io — 100 free requests/month, generous tier"
    );

    companion object {
        fun fromId(id: String): PhoneApiProvider? = entries.firstOrNull { it.id == id }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  ApiUsageTracker
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Tracks API call quotas across free-tier phone validation providers,
 * preventing accidental rate-limit exhaustion.
 *
 * Storage: plain [SharedPreferences] — tiny data, no migration concerns.
 * All keys are namespaced per provider and per cycle (year-month), so quotas
 * automatically "reset" when the calendar month rolls over without any
 * explicit database migration.
 *
 * Thread-safety: SharedPreferences commits are synchronous on the calling
 * coroutine; no additional locking is required for the single-writer pattern
 * used here.
 */
@Singleton
class ApiUsageTracker @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // ── 1. Increment ──────────────────────────────────────────────────────────

    /**
     * Records one API call for [provider].
     * Auto-resets the counter when the stored cycle month differs from today's.
     */
    fun incrementUsage(provider: String) {
        resetQuotaIfNeeded(provider)
        val key     = usageKey(provider)
        val current = prefs.getInt(key, 0)
        prefs.edit().putInt(key, current + 1).apply()
    }

    // ── 2. Remaining quota ────────────────────────────────────────────────────

    /** Returns the number of calls still available for [provider] this month. */
    fun getRemainingQuota(provider: String): Int {
        resetQuotaIfNeeded(provider)
        val total = PhoneApiProvider.fromId(provider)?.monthlyQuota ?: return 0
        val used  = prefs.getInt(usageKey(provider), 0)
        return (total - used).coerceAtLeast(0)
    }

    // ── 3. Can use ────────────────────────────────────────────────────────────

    /**
     * Returns true when [provider] is still within its monthly quota and
     * has not been administratively disabled.
     */
    fun canUseProvider(provider: String): Boolean {
        if (PhoneApiProvider.fromId(provider) == null) return false
        if (!prefs.getBoolean(activeKey(provider), true)) return false
        return getRemainingQuota(provider) > 0
    }

    // ── 4. Reset if needed ────────────────────────────────────────────────────

    /**
     * Resets the usage counter for [provider] when the stored cycle key
     * (year-month string) differs from the current month.
     *
     * This is the primary reset mechanism — no background job required.
     */
    fun resetQuotaIfNeeded(provider: String) {
        val cycleKey     = "cycle_$provider"
        val currentCycle = currentCycleKey()
        val storedCycle  = prefs.getString(cycleKey, "") ?: ""

        if (storedCycle != currentCycle) {
            prefs.edit()
                .putInt(usageKey(provider), 0)
                .putString(cycleKey, currentCycle)
                .putLong(resetDateKey(provider), nextResetEpoch())
                .apply()
        }
    }

    // ── 5. Best available provider ────────────────────────────────────────────

    /**
     * Returns the [PhoneApiProvider.id] of the provider with the lowest
     * [PhoneApiProvider.priority] that still has remaining quota.
     *
     * Returns `null` when all providers are exhausted or disabled — the caller
     * should fall back to offline-only analysis.
     */
    fun getBestAvailableProvider(): String? =
        PhoneApiProvider.entries
            .sortedBy { it.priority }
            .firstOrNull { canUseProvider(it.id) }
            ?.id

    // ── 6. All quotas ─────────────────────────────────────────────────────────

    /** Returns a snapshot [ApiQuota] for every known provider. */
    fun getAllQuotas(): List<ApiQuota> = PhoneApiProvider.entries.map { provider ->
        resetQuotaIfNeeded(provider.id)
        val used = prefs.getInt(usageKey(provider.id), 0)
        ApiQuota(
            providerName = provider.id,
            totalQuota   = provider.monthlyQuota,
            usedQuota    = used,
            resetDate    = prefs.getLong(resetDateKey(provider.id), nextResetEpoch()),
            isActive     = prefs.getBoolean(activeKey(provider.id), true)
        )
    }

    // ── 7. Low-quota notification helper ─────────────────────────────────────

    /**
     * Returns providers whose usage has crossed the [ApiQuota.LOW_THRESHOLD].
     * The caller (ViewModel / Repository) decides how to surface this to the user.
     */
    fun getLowQuotaProviders(): List<ApiQuota> =
        getAllQuotas().filter { it.isLow && !it.isExhausted }

    /**
     * Returns providers that have fully exhausted their monthly quota.
     */
    fun getExhaustedProviders(): List<ApiQuota> =
        getAllQuotas().filter { it.isExhausted }

    // ── 8. Admin helpers ──────────────────────────────────────────────────────

    /** Administratively enable / disable [provider] without touching its counter. */
    fun setProviderActive(provider: String, active: Boolean) {
        prefs.edit().putBoolean(activeKey(provider), active).apply()
    }

    /** Hard-resets usage counter for [provider] (useful for testing). */
    fun forceReset(provider: String) {
        prefs.edit()
            .putInt(usageKey(provider), 0)
            .putString("cycle_$provider", currentCycleKey())
            .putLong(resetDateKey(provider), nextResetEpoch())
            .apply()
    }

    // ── Internal key helpers ──────────────────────────────────────────────────

    private fun usageKey(provider: String)     = "usage_$provider"
    private fun activeKey(provider: String)    = "active_$provider"
    private fun resetDateKey(provider: String) = "reset_$provider"

    /**
     * Returns a "YYYY-MM" string for the current month.
     * When this string changes (month rollover) the counter is auto-reset.
     */
    private fun currentCycleKey(): String {
        val cal = java.util.Calendar.getInstance()
        val y   = cal.get(java.util.Calendar.YEAR)
        val m   = cal.get(java.util.Calendar.MONTH) + 1   // 0-indexed → 1-indexed
        return "$y-${m.toString().padStart(2, '0')}"
    }

    /**
     * Epoch-ms timestamp for the 1st day of next month at 00:00:00 UTC.
     * Stored so the UI can display a "resets on …" date.
     */
    private fun nextResetEpoch(): Long {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        cal.add(java.util.Calendar.MONTH, 1)
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private companion object {
        const val PREFS_NAME = "shadowinspect_api_quotas"
    }

    fun getAllQuotaStatus(): List<QuotaStatus> {
        return getAllQuotas().map { quota ->
            checkAndResetQuota(quota.providerName)
            QuotaStatus(
                providerName = quota.providerName,
                remainingQuota = quota.remainingQuota,
                percentageUsed = (quota.usagePercent * 100).toInt(),
                resetInDays = ((quota.resetDate - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).toInt().coerceAtLeast(0),
                isAvailable = quota.remainingQuota > 0
            )
        }
    }

    private fun checkAndResetQuota(provider: String) {
        resetQuotaIfNeeded(provider)
    }
}
