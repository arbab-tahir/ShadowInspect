package com.shadowinspect.app.domain.phone

import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────────────
//  PhoneNumberFormatter
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Handles all phone-number formatting, validation, and parsing for the phone
 * scanner module.
 *
 * Deliberately free of external libraries (libphonenumber adds ~1 MB AAR) —
 * uses the [CountryDataProvider] dataset for country-specific rules instead.
 */
@Singleton
class PhoneNumberFormatter @Inject constructor(
    private val countryDataProvider: CountryDataProvider
) {

    // ── 1. Format ─────────────────────────────────────────────────────────────

    /**
     * Returns a human-readable international representation of [number].
     *
     * Algorithm:
     * 1. Strip all non-digit characters (keep a leading '+').
     * 2. Try to detect the country from the leading digits.
     * 3. Apply country-specific formatting if a rule exists; otherwise fall
     *    back to a generic grouped format.
     *
     * Examples:
     * ```
     * "+12025551234"  → "+1 (202) 555-1234"
     * "+447911123456" → "+44 7911 123456"
     * "+919876543210" → "+91 98765 43210"
     * "2025551234"    → "+1 (202) 555-1234"  (countryCode = "US")
     * ```
     *
     * @param number      Raw number string (any format).
     * @param countryCode Hint ISO-3166 code used when [number] has no leading '+'.
     */
    fun formatNumber(number: String, countryCode: String? = null): String {
        val stripped = stripToDigitsAndPlus(number)
        if (stripped.isEmpty()) return number.trim()

        // Ensure the number has a leading '+' so we can match dial codes
        val withPlus = ensurePlusPrefix(stripped, countryCode)

        val country = detectCountry(withPlus)

        return if (country != null) {
            applyCountryFormat(withPlus, country)
        } else {
            genericFormat(withPlus)
        }
    }

    // ── 2. Validate ───────────────────────────────────────────────────────────

    /**
     * Returns true when [number] appears to be a plausible phone number.
     *
     * Checks (in order):
     * 1. Contains only digits, spaces, hyphens, parentheses, and an optional
     *    leading '+'.
     * 2. Total digit count is within the E.164 range [7, 15].
     * 3. If a matching [Country] can be detected, validates against its
     *    `minLength`/`maxLength` (subscriber part only, without dial code).
     */
    fun validateNumber(number: String, countryCodeHint: String? = null): Boolean {
        if (!hasValidCharacters(number)) return false

        val digits = number.filter { it.isDigit() }
        if (digits.length !in 7..15) return false

        val withPlus = ensurePlusPrefix(number.filter { it.isDigit() || it == '+' }, countryCodeHint)
        val country = detectCountry(withPlus) ?: return true   // generic pass

        if (countryCodeHint != null) {
            val hintCountry = countryDataProvider.getCountryByCode(countryCodeHint)
            if (hintCountry != null && !withPlus.startsWith(hintCountry.dialCode)) {
                return false
            }
        }

        val subscriberDigits = withPlus.removePrefix(country.dialCode).length
        return subscriberDigits in country.minLength..country.maxLength
    }

    // ── 3. Get number without country code ────────────────────────────────────

    /**
     * Returns only the subscriber portion (national number) after stripping
     * the detected dial code.
     *
     * Example: "+12025551234" → "2025551234"
     */
    fun getNumberWithoutCountryCode(number: String): String {
        val stripped = stripToDigitsAndPlus(number)
        val withPlus = ensurePlusPrefix(stripped)
        val country = detectCountry(withPlus) ?: return stripped.trimStart('+')
        return withPlus.removePrefix(country.dialCode)
    }

    // ── 4. Extract country code ───────────────────────────────────────────────

    /**
     * Returns the dial code detected from [number], e.g. "+1", "+44", "+91".
     * Returns null when no match can be found.
     */
    fun extractCountryCode(number: String): String? {
        val withPlus = ensurePlusPrefix(stripToDigitsAndPlus(number))
        return detectCountry(withPlus)?.dialCode
    }

    // ── 5. Mask for privacy ───────────────────────────────────────────────────

    /**
     * Returns a privacy-masked version of [number] suitable for logging or
     * display in lists.
     *
     * Format: keeps the dial code + first 2 subscriber digits and last 2
     * subscriber digits, replaces the rest with '*'.
     *
     * Example: "+12025551234" → "+1 20*****34"
     */
    fun maskNumber(number: String): String {
        val stripped = stripToDigitsAndPlus(number)
        if (stripped.length < 6) return "****"

        val withPlus = ensurePlusPrefix(stripped)
        val country = detectCountry(withPlus)
        val dialCode = country?.dialCode ?: ""
        val subscriber = if (dialCode.isNotEmpty()) withPlus.removePrefix(dialCode)
                         else withPlus.trimStart('+')

        if (subscriber.length <= 4) return "$dialCode****"

        val visible = 2
        val masked = "*".repeat((subscriber.length - visible * 2).coerceAtLeast(2))
        return "$dialCode ${subscriber.take(visible)}$masked${subscriber.takeLast(visible)}"
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /** Strips all characters except digits and a single leading '+'. */
    private fun stripToDigitsAndPlus(input: String): String {
        val trimmed = input.trim()
        val hasPlus = trimmed.startsWith('+')
        val digits  = trimmed.filter { it.isDigit() }
        return if (hasPlus) "+$digits" else digits
    }

    /** Adds a '+' prefix if missing. Falls back to country's dial code when given. */
    private fun ensurePlusPrefix(stripped: String, countryCode: String? = null): String {
        if (stripped.startsWith('+')) return stripped
        // If a country hint is provided, prepend its dial code
        if (countryCode != null) {
            val hint = countryDataProvider.getCountryByCode(countryCode)
            if (hint != null) return "${hint.dialCode}$stripped"
        }
        return "+$stripped"
    }

    /**
     * Tries to match the leading digits of [withPlus] to a known dial code.
     * Longest-match wins (e.g. "+1868" matches Trinidad "+1" but "+44207" matches UK "+44").
     * We try 4-digit, 3-digit, 2-digit, 1-digit prefixes in that order.
     */
    private fun detectCountry(withPlus: String): Country? {
        for (len in 4 downTo 1) {
            val prefix = withPlus.take(len + 1)   // +1 for the '+' character
            val match  = countryDataProvider.getCountryByDialCode(prefix)
            if (match != null) return match
        }
        return null
    }

    /** Returns true when the string contains only phone-valid characters. */
    private fun hasValidCharacters(number: String): Boolean =
        number.trim().all { it.isDigit() || it in setOf('+', '-', ' ', '(', ')', '.') }

    // ── Country-specific formatters ───────────────────────────────────────────

    /**
     * Routes to a country-specific formatter. Falls back to [genericFormat]
     * when no dedicated rule exists.
     */
    private fun applyCountryFormat(withPlus: String, country: Country): String {
        val subscriber = withPlus.removePrefix(country.dialCode)
        return when (country.code) {
            // ── US / Canada / most NANP (+1, 10 digits: NXX-NXX-XXXX) ────────
            "US", "CA", "PR", "JM", "TT", "BB", "BS", "DO",
            "LC", "VC", "GD", "AG", "DM", "KN", "GU", "VI",
            "AS", "MP", "AI", "MS", "TC", "KY", "VG", "SX",
            "BM", "PN" -> {
                if (subscriber.length == 10)
                    "${country.dialCode} (${subscriber.take(3)}) ${subscriber.substring(3, 6)}-${subscriber.substring(6)}"
                else genericFormat(withPlus)
            }

            // ── UK (+44, national format 0xxxx xxxxxx) ────────────────────────
            "GB", "GG", "JE", "IM" -> {
                when (subscriber.length) {
                    10 -> "${country.dialCode} ${subscriber.take(4)} ${subscriber.substring(4, 7)} ${subscriber.substring(7)}"
                    11 -> "${country.dialCode} ${subscriber.take(5)} ${subscriber.substring(5)}"
                    else -> genericFormat(withPlus)
                }
            }

            // ── India (+91, 10 digits: NXXXX XXXXX) ───────────────────────────
            "IN" -> {
                if (subscriber.length == 10)
                    "${country.dialCode} ${subscriber.take(5)} ${subscriber.substring(5)}"
                else genericFormat(withPlus)
            }

            // ── Pakistan (+92, 10 digits: 3XX XXXXXXX) ───────────────────────
            "PK" -> {
                if (subscriber.length == 10)
                    "${country.dialCode} ${subscriber.take(3)} ${subscriber.substring(3)}"
                else genericFormat(withPlus)
            }

            // ── Australia (+61, 9 digits: X XXXX XXXX) ────────────────────────
            "AU" -> {
                if (subscriber.length == 9)
                    "${country.dialCode} ${subscriber.take(1)} ${subscriber.substring(1, 5)} ${subscriber.substring(5)}"
                else genericFormat(withPlus)
            }

            // ── Germany (+49, variable length) ────────────────────────────────
            "DE" -> {
                val s = subscriber
                when {
                    s.length == 10 -> "${country.dialCode} ${s.take(3)} ${s.substring(3, 6)} ${s.substring(6)}"
                    s.length == 11 -> "${country.dialCode} ${s.take(4)} ${s.substring(4, 7)} ${s.substring(7)}"
                    else -> genericFormat(withPlus)
                }
            }

            // ── France (+33, 9 digits: X XX XX XX XX) ─────────────────────────
            "FR" -> {
                if (subscriber.length == 9)
                    "${country.dialCode} ${subscriber.chunked(2).joinToString(" ")}"
                else genericFormat(withPlus)
            }

            // ── China (+86, 11 digits: XXX XXXX XXXX) ─────────────────────────
            "CN" -> {
                if (subscriber.length == 11)
                    "${country.dialCode} ${subscriber.take(3)} ${subscriber.substring(3, 7)} ${subscriber.substring(7)}"
                else genericFormat(withPlus)
            }

            // ── Brazil (+55, 11 digits: XX XXXXX-XXXX) ────────────────────────
            "BR" -> {
                when (subscriber.length) {
                    10 -> "${country.dialCode} (${subscriber.take(2)}) ${subscriber.substring(2, 6)}-${subscriber.substring(6)}"
                    11 -> "${country.dialCode} (${subscriber.take(2)}) ${subscriber.substring(2, 7)}-${subscriber.substring(7)}"
                    else -> genericFormat(withPlus)
                }
            }

            // ── All others: generic grouped format ────────────────────────────
            else -> genericFormat(withPlus)
        }
    }

    /**
     * Generic fallback: splits the subscriber number into groups of 3
     * (last group absorbs remainders).
     *
     * Example: "+919876543210" → "+91 987 654 3210"
     */
    private fun genericFormat(withPlus: String): String {
        val country = detectCountry(withPlus)
        val dialCode = country?.dialCode ?: "+"
        val subscriber = if (country != null) withPlus.removePrefix(dialCode)
                         else withPlus.trimStart('+')
        if (subscriber.isEmpty()) return withPlus

        val grouped = subscriber.chunked(3).joinToString(" ")
        return "$dialCode $grouped"
    }
}
