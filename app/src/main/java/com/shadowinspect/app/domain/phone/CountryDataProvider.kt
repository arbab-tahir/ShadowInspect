package com.shadowinspect.app.domain.phone

import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────────────
//  Country – domain model
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Represents a country's telephony metadata.
 *
 * @param code      ISO 3166-1 alpha-2 country code, e.g. "US".
 * @param name      Full English name, e.g. "United States".
 * @param dialCode  ITU-T dial prefix including '+', e.g. "+1".
 * @param flagEmoji Unicode regional-indicator flag emoji, e.g. "🇺🇸".
 * @param minLength Minimum subscriber-number length (digits, without country code).
 * @param maxLength Maximum subscriber-number length (digits, without country code).
 * @param priority  Display priority — higher values appear first in pickers.
 *                  10 = most common, 5 = European, 0 = all others.
 */
data class Country(
    val code: String,
    val name: String,
    val dialCode: String,
    val flagEmoji: String,
    val minLength: Int,
    val maxLength: Int,
    val priority: Int = 0
)

// ─────────────────────────────────────────────────────────────────────────────
//  CountryDataProvider
// ─────────────────────────────────────────────────────────────────────────────

@Singleton
class CountryDataProvider @Inject constructor() {

    // ── Master dataset ────────────────────────────────────────────────────────
    // Sorted lazily (once) by descending priority then ascending name.
    private val allCountriesSorted: List<Country> by lazy {
        buildCountryList().sortedWith(compareByDescending<Country> { it.priority }.thenBy { it.name })
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Returns the complete country list ordered by priority (desc) then name (asc). */
    fun getAllCountries(): List<Country> = allCountriesSorted

    /** Exact lookup by ISO 3166-1 alpha-2 [code] (case-insensitive). */
    fun getCountryByCode(code: String): Country? =
        allCountriesSorted.firstOrNull { it.code.equals(code, ignoreCase = true) }

    /**
     * Lookup by dial code string, e.g. "+1", "44", "+91".
     * When multiple countries share a dial code (e.g. +1 → US & CA), the one
     * with the highest priority is returned.
     */
    fun getCountryByDialCode(dialCode: String): Country? {
        val normalised = if (dialCode.startsWith("+")) dialCode else "+$dialCode"
        return allCountriesSorted.firstOrNull { it.dialCode == normalised }
    }

    /**
     * Case-insensitive search over country name, ISO code, and dial code.
     * Returns results in the standard priority-then-name order.
     */
    fun searchCountries(query: String): List<Country> {
        if (query.isBlank()) return allCountriesSorted
        val q = query.trim().lowercase()
        return allCountriesSorted.filter { country ->
            country.name.lowercase().contains(q) ||
            country.code.lowercase().contains(q) ||
            country.dialCode.contains(q)
        }
    }

    // ── Dataset builder ───────────────────────────────────────────────────────

    private fun buildCountryList(): List<Country> = listOf(

        // ══ Priority 10 – highest-traffic countries ═══════════════════════════
        Country("US", "United States",        "+1",    "🇺🇸", 10, 10, priority = 10),
        Country("GB", "United Kingdom",       "+44",   "🇬🇧", 10, 11, priority = 10),
        Country("IN", "India",                "+91",   "🇮🇳", 10, 10, priority = 10),
        Country("CA", "Canada",               "+1",    "🇨🇦", 10, 10, priority = 10),
        Country("AU", "Australia",            "+61",   "🇦🇺",  9,  9, priority = 10),
        Country("PK", "Pakistan",             "+92",   "🇵🇰", 10, 10, priority = 10),

        // ══ Priority 5 – European countries ══════════════════════════════════
        Country("DE", "Germany",              "+49",   "🇩🇪", 10, 12, priority = 5),
        Country("FR", "France",               "+33",   "🇫🇷",  9,  9, priority = 5),
        Country("IT", "Italy",                "+39",   "🇮🇹",  9, 11, priority = 5),
        Country("ES", "Spain",                "+34",   "🇪🇸",  9,  9, priority = 5),
        Country("NL", "Netherlands",          "+31",   "🇳🇱",  9,  9, priority = 5),
        Country("BE", "Belgium",              "+32",   "🇧🇪",  8,  9, priority = 5),
        Country("PT", "Portugal",             "+351",  "🇵🇹",  9,  9, priority = 5),
        Country("SE", "Sweden",               "+46",   "🇸🇪",  7,  9, priority = 5),
        Country("NO", "Norway",               "+47",   "🇳🇴",  8,  8, priority = 5),
        Country("DK", "Denmark",              "+45",   "🇩🇰",  8,  8, priority = 5),
        Country("FI", "Finland",              "+358",  "🇫🇮",  9, 10, priority = 5),
        Country("CH", "Switzerland",          "+41",   "🇨🇭",  9,  9, priority = 5),
        Country("AT", "Austria",              "+43",   "🇦🇹", 10, 13, priority = 5),
        Country("PL", "Poland",               "+48",   "🇵🇱",  9,  9, priority = 5),
        Country("CZ", "Czech Republic",       "+420",  "🇨🇿",  9,  9, priority = 5),
        Country("SK", "Slovakia",             "+421",  "🇸🇰",  9,  9, priority = 5),
        Country("HU", "Hungary",              "+36",   "🇭🇺",  9,  9, priority = 5),
        Country("RO", "Romania",              "+40",   "🇷🇴", 10, 10, priority = 5),
        Country("BG", "Bulgaria",             "+359",  "🇧🇬",  8,  9, priority = 5),
        Country("HR", "Croatia",              "+385",  "🇭🇷",  8,  9, priority = 5),
        Country("RS", "Serbia",               "+381",  "🇷🇸",  8,  9, priority = 5),
        Country("GR", "Greece",               "+30",   "🇬🇷", 10, 10, priority = 5),
        Country("IE", "Ireland",              "+353",  "🇮🇪",  7,  9, priority = 5),
        Country("UA", "Ukraine",              "+380",  "🇺🇦",  9,  9, priority = 5),
        Country("RU", "Russia",               "+7",    "🇷🇺", 10, 10, priority = 5),
        Country("TR", "Turkey",               "+90",   "🇹🇷", 10, 10, priority = 5),

        // ══ Priority 0 – rest of the world ═══════════════════════════════════

        // ── Asia-Pacific ──────────────────────────────────────────────────────
        Country("CN", "China",                "+86",   "🇨🇳", 11, 11),
        Country("JP", "Japan",                "+81",   "🇯🇵", 10, 11),
        Country("KR", "South Korea",          "+82",   "🇰🇷",  9, 11),
        Country("ID", "Indonesia",            "+62",   "🇮🇩",  9, 12),
        Country("MY", "Malaysia",             "+60",   "🇲🇾",  9, 10),
        Country("SG", "Singapore",            "+65",   "🇸🇬",  8,  8),
        Country("TH", "Thailand",             "+66",   "🇹🇭",  9,  9),
        Country("VN", "Vietnam",              "+84",   "🇻🇳",  9, 10),
        Country("PH", "Philippines",          "+63",   "🇵🇭", 10, 10),
        Country("BD", "Bangladesh",           "+880",  "🇧🇩", 10, 10),
        Country("NP", "Nepal",                "+977",  "🇳🇵",  9, 10),
        Country("LK", "Sri Lanka",            "+94",   "🇱🇰",  9,  9),
        Country("MM", "Myanmar",              "+95",   "🇲🇲",  8, 10),
        Country("KH", "Cambodia",             "+855",  "🇰🇭",  8,  9),
        Country("LA", "Laos",                 "+856",  "🇱🇦",  8,  9),
        Country("MN", "Mongolia",             "+976",  "🇲🇳",  8,  8),
        Country("TW", "Taiwan",               "+886",  "🇹🇼",  9,  9),
        Country("HK", "Hong Kong",            "+852",  "🇭🇰",  8,  8),
        Country("MO", "Macau",                "+853",  "🇲🇴",  8,  8),
        Country("BT", "Bhutan",               "+975",  "🇧🇹",  7,  8),
        Country("MV", "Maldives",             "+960",  "🇲🇻",  7,  7),
        Country("AF", "Afghanistan",          "+93",   "🇦🇫",  9,  9),
        Country("NZ", "New Zealand",          "+64",   "🇳🇿",  8,  9),
        Country("FJ", "Fiji",                 "+679",  "🇫🇯",  7,  7),
        Country("PG", "Papua New Guinea",     "+675",  "🇵🇬",  7,  8),
        Country("SB", "Solomon Islands",      "+677",  "🇸🇧",  7,  7),
        Country("VU", "Vanuatu",              "+678",  "🇻🇺",  7,  7),
        Country("TO", "Tonga",                "+676",  "🇹🇴",  5,  7),
        Country("WS", "Samoa",                "+685",  "🇼🇸",  5,  7),
        Country("KI", "Kiribati",             "+686",  "🇰🇮",  5,  8),
        Country("TV", "Tuvalu",               "+688",  "🇹🇻",  5,  6),
        Country("NR", "Nauru",                "+674",  "🇳🇷",  7,  7),
        Country("PW", "Palau",                "+680",  "🇵🇼",  7,  7),
        Country("FM", "Micronesia",           "+691",  "🇫🇲",  7,  7),
        Country("MH", "Marshall Islands",     "+692",  "🇲🇭",  7,  7),
        Country("CK", "Cook Islands",         "+682",  "🇨🇰",  5,  5),

        // ── Middle East ───────────────────────────────────────────────────────
        Country("SA", "Saudi Arabia",         "+966",  "🇸🇦",  9,  9),
        Country("AE", "United Arab Emirates", "+971",  "🇦🇪",  9,  9),
        Country("QA", "Qatar",                "+974",  "🇶🇦",  8,  8),
        Country("KW", "Kuwait",               "+965",  "🇰🇼",  8,  8),
        Country("BH", "Bahrain",              "+973",  "🇧🇭",  8,  8),
        Country("OM", "Oman",                 "+968",  "🇴🇲",  8,  8),
        Country("JO", "Jordan",               "+962",  "🇯🇴",  9,  9),
        Country("LB", "Lebanon",              "+961",  "🇱🇧",  7,  8),
        Country("IQ", "Iraq",                 "+964",  "🇮🇶", 10, 10),
        Country("IR", "Iran",                 "+98",   "🇮🇷", 10, 10),
        Country("SY", "Syria",                "+963",  "🇸🇾",  9,  9),
        Country("YE", "Yemen",                "+967",  "🇾🇪",  9,  9),
        Country("PS", "Palestine",            "+970",  "🇵🇸",  9,  9),
        Country("IL", "Israel",               "+972",  "🇮🇱",  9,  9),

        // ── Central Asia ──────────────────────────────────────────────────────
        Country("KZ", "Kazakhstan",           "+7",    "🇰🇿", 10, 10),
        Country("UZ", "Uzbekistan",           "+998",  "🇺🇿",  9,  9),
        Country("TM", "Turkmenistan",         "+993",  "🇹🇲",  8,  8),
        Country("TJ", "Tajikistan",           "+992",  "🇹🇯",  9,  9),
        Country("KG", "Kyrgyzstan",           "+996",  "🇰🇬",  9,  9),
        Country("AZ", "Azerbaijan",           "+994",  "🇦🇿",  9,  9),
        Country("AM", "Armenia",              "+374",  "🇦🇲",  8,  8),
        Country("GE", "Georgia",              "+995",  "🇬🇪",  9,  9),

        // ── Eastern Europe / Balkans ──────────────────────────────────────────
        Country("BY", "Belarus",              "+375",  "🇧🇾",  9,  9),
        Country("MD", "Moldova",              "+373",  "🇲🇩",  8,  8),
        Country("AL", "Albania",              "+355",  "🇦🇱",  9,  9),
        Country("BA", "Bosnia and Herzegovina","+387", "🇧🇦",  8,  8),
        Country("MK", "North Macedonia",      "+389",  "🇲🇰",  8,  8),
        Country("ME", "Montenegro",           "+382",  "🇲🇪",  8,  8),
        Country("XK", "Kosovo",               "+383",  "🇽🇰",  8,  8),
        Country("SI", "Slovenia",             "+386",  "🇸🇮",  8,  8),
        Country("EE", "Estonia",              "+372",  "🇪🇪",  7,  8),
        Country("LV", "Latvia",               "+371",  "🇱🇻",  8,  8),
        Country("LT", "Lithuania",            "+370",  "🇱🇹",  8,  8),
        Country("LU", "Luxembourg",           "+352",  "🇱🇺",  9, 11),
        Country("MT", "Malta",                "+356",  "🇲🇹",  8,  8),
        Country("CY", "Cyprus",               "+357",  "🇨🇾",  8,  8),
        Country("IS", "Iceland",              "+354",  "🇮🇸",  7,  7),
        Country("LI", "Liechtenstein",        "+423",  "🇱🇮",  7,  9),
        Country("MC", "Monaco",               "+377",  "🇲🇨",  8,  9),
        Country("SM", "San Marino",           "+378",  "🇸🇲",  6, 10),
        Country("AD", "Andorra",              "+376",  "🇦🇩",  6,  9),
        Country("VA", "Vatican City",         "+379",  "🇻🇦",  6, 10),

        // ── Africa – North ────────────────────────────────────────────────────
        Country("EG", "Egypt",                "+20",   "🇪🇬", 10, 10),
        Country("LY", "Libya",                "+218",  "🇱🇾",  9,  9),
        Country("TN", "Tunisia",              "+216",  "🇹🇳",  8,  8),
        Country("DZ", "Algeria",              "+213",  "🇩🇿",  9,  9),
        Country("MA", "Morocco",              "+212",  "🇲🇦",  9,  9),
        Country("SD", "Sudan",                "+249",  "🇸🇩",  9,  9),
        Country("SS", "South Sudan",          "+211",  "🇸🇸",  9,  9),

        // ── Africa – West ─────────────────────────────────────────────────────
        Country("NG", "Nigeria",              "+234",  "🇳🇬", 10, 11),
        Country("GH", "Ghana",                "+233",  "🇬🇭",  9,  9),
        Country("SN", "Senegal",              "+221",  "🇸🇳",  9,  9),
        Country("CI", "Côte d'Ivoire",        "+225",  "🇨🇮", 10, 10),
        Country("CM", "Cameroon",             "+237",  "🇨🇲",  9,  9),
        Country("GN", "Guinea",               "+224",  "🇬🇳",  9,  9),
        Country("ML", "Mali",                 "+223",  "🇲🇱",  8,  8),
        Country("BF", "Burkina Faso",         "+226",  "🇧🇫",  8,  8),
        Country("NE", "Niger",                "+227",  "🇳🇪",  8,  8),
        Country("MR", "Mauritania",           "+222",  "🇲🇷",  8,  8),
        Country("TG", "Togo",                 "+228",  "🇹🇬",  8,  8),
        Country("BJ", "Benin",                "+229",  "🇧🇯",  8,  8),
        Country("GW", "Guinea-Bissau",        "+245",  "🇬🇼",  7,  7),
        Country("SL", "Sierra Leone",         "+232",  "🇸🇱",  8,  8),
        Country("LR", "Liberia",              "+231",  "🇱🇷",  7,  8),
        Country("GM", "Gambia",               "+220",  "🇬🇲",  7,  7),
        Country("CV", "Cape Verde",           "+238",  "🇨🇻",  7,  7),
        Country("ST", "São Tomé and Príncipe","+239",  "🇸🇹",  7,  7),
        Country("GQ", "Equatorial Guinea",    "+240",  "🇬🇶",  9,  9),
        Country("GA", "Gabon",                "+241",  "🇬🇦",  7,  8),
        Country("CG", "Congo",                "+242",  "🇨🇬",  9,  9),
        Country("CD", "DR Congo",             "+243",  "🇨🇩",  9,  9),
        Country("CF", "Central African Republic","+236","🇨🇫", 8,  8),
        Country("TD", "Chad",                 "+235",  "🇹🇩",  8,  8),

        // ── Africa – East ─────────────────────────────────────────────────────
        Country("ET", "Ethiopia",             "+251",  "🇪🇹",  9,  9),
        Country("KE", "Kenya",                "+254",  "🇰🇪",  9, 10),
        Country("TZ", "Tanzania",             "+255",  "🇹🇿",  9,  9),
        Country("UG", "Uganda",               "+256",  "🇺🇬",  9,  9),
        Country("RW", "Rwanda",               "+250",  "🇷🇼",  9,  9),
        Country("BI", "Burundi",              "+257",  "🇧🇮",  8,  8),
        Country("DJ", "Djibouti",             "+253",  "🇩🇯",  8,  8),
        Country("SO", "Somalia",              "+252",  "🇸🇴",  8,  9),
        Country("ER", "Eritrea",              "+291",  "🇪🇷",  7,  7),
        Country("MG", "Madagascar",           "+261",  "🇲🇬",  9,  9),
        Country("SC", "Seychelles",           "+248",  "🇸🇨",  7,  7),
        Country("MU", "Mauritius",            "+230",  "🇲🇺",  7,  8),
        Country("KM", "Comoros",              "+269",  "🇰🇲",  7,  7),

        // ── Africa – Southern ─────────────────────────────────────────────────
        Country("ZA", "South Africa",         "+27",   "🇿🇦",  9,  9),
        Country("ZW", "Zimbabwe",             "+263",  "🇿🇼",  9,  9),
        Country("ZM", "Zambia",               "+260",  "🇿🇲",  9,  9),
        Country("MW", "Malawi",               "+265",  "🇲🇼",  9,  9),
        Country("MZ", "Mozambique",           "+258",  "🇲🇿",  9,  9),
        Country("AO", "Angola",               "+244",  "🇦🇴",  9,  9),
        Country("NA", "Namibia",              "+264",  "🇳🇦",  9,  9),
        Country("BW", "Botswana",             "+267",  "🇧🇼",  7,  8),
        Country("LS", "Lesotho",              "+266",  "🇱🇸",  8,  8),
        Country("SZ", "Eswatini",             "+268",  "🇸🇿",  8,  8),

        // ── Americas – Latin & Caribbean ──────────────────────────────────────
        Country("MX", "Mexico",               "+52",   "🇲🇽", 10, 10),
        Country("BR", "Brazil",               "+55",   "🇧🇷", 10, 11),
        Country("AR", "Argentina",            "+54",   "🇦🇷", 10, 10),
        Country("CO", "Colombia",             "+57",   "🇨🇴", 10, 10),
        Country("VE", "Venezuela",            "+58",   "🇻🇪", 10, 10),
        Country("PE", "Peru",                 "+51",   "🇵🇪",  9,  9),
        Country("CL", "Chile",                "+56",   "🇨🇱",  9,  9),
        Country("EC", "Ecuador",              "+593",  "🇪🇨",  9,  9),
        Country("BO", "Bolivia",              "+591",  "🇧🇴",  8,  8),
        Country("PY", "Paraguay",             "+595",  "🇵🇾",  9,  9),
        Country("UY", "Uruguay",              "+598",  "🇺🇾",  8,  9),
        Country("GY", "Guyana",               "+592",  "🇬🇾",  7,  7),
        Country("SR", "Suriname",             "+597",  "🇸🇷",  7,  7),
        Country("GF", "French Guiana",        "+594",  "🇬🇫",  9,  9),
        Country("CU", "Cuba",                 "+53",   "🇨🇺",  8,  8),
        Country("DO", "Dominican Republic",   "+1",    "🇩🇴", 10, 10),
        Country("HT", "Haiti",                "+509",  "🇭🇹",  8,  8),
        Country("JM", "Jamaica",              "+1",    "🇯🇲", 10, 10),
        Country("TT", "Trinidad and Tobago",  "+1",    "🇹🇹", 10, 10),
        Country("BB", "Barbados",             "+1",    "🇧🇧", 10, 10),
        Country("LC", "Saint Lucia",          "+1",    "🇱🇨", 10, 10),
        Country("VC", "Saint Vincent",        "+1",    "🇻🇨", 10, 10),
        Country("GD", "Grenada",              "+1",    "🇬🇩", 10, 10),
        Country("AG", "Antigua and Barbuda",  "+1",    "🇦🇬", 10, 10),
        Country("DM", "Dominica",             "+1",    "🇩🇲", 10, 10),
        Country("KN", "Saint Kitts and Nevis","+1",    "🇰🇳", 10, 10),
        Country("BS", "Bahamas",              "+1",    "🇧🇸", 10, 10),
        Country("BZ", "Belize",               "+501",  "🇧🇿",  7,  7),
        Country("GT", "Guatemala",            "+502",  "🇬🇹",  8,  8),
        Country("HN", "Honduras",             "+504",  "🇭🇳",  8,  8),
        Country("SV", "El Salvador",          "+503",  "🇸🇻",  8,  8),
        Country("NI", "Nicaragua",            "+505",  "🇳🇮",  8,  8),
        Country("CR", "Costa Rica",           "+506",  "🇨🇷",  8,  8),
        Country("PA", "Panama",               "+507",  "🇵🇦",  8,  8),
        Country("PR", "Puerto Rico",          "+1",    "🇵🇷", 10, 10),
        Country("TC", "Turks and Caicos",     "+1",    "🇹🇨", 10, 10),
        Country("KY", "Cayman Islands",       "+1",    "🇰🇾", 10, 10),
        Country("VG", "British Virgin Islands","+1",   "🇻🇬", 10, 10),
        Country("VI", "US Virgin Islands",    "+1",    "🇻🇮", 10, 10),
        Country("AW", "Aruba",                "+297",  "🇦🇼",  7,  7),
        Country("CW", "Curaçao",              "+599",  "🇨🇼",  7,  8),
        Country("SX", "Sint Maarten",         "+1",    "🇸🇽", 10, 10),
        Country("BQ", "Bonaire",              "+599",  "🇧🇶",  7,  7),
        Country("MF", "Saint Martin",         "+590",  "🇲🇫",  9,  9),
        Country("GP", "Guadeloupe",           "+590",  "🇬🇵",  9,  9),
        Country("MQ", "Martinique",           "+596",  "🇲🇶",  9,  9),
        Country("PM", "Saint Pierre & Miquelon","+508","🇵🇲",  6,  6),

        // ── Oceania (remaining) ───────────────────────────────────────────────
        Country("NC", "New Caledonia",        "+687",  "🇳🇨",  6,  6),
        Country("PF", "French Polynesia",     "+689",  "🇵🇫",  6,  6),
        Country("WF", "Wallis and Futuna",    "+681",  "🇼🇫",  6,  6),
        Country("GU", "Guam",                 "+1",    "🇬🇺", 10, 10),
        Country("MP", "Northern Mariana Islands","+1", "🇲🇵", 10, 10),
        Country("AS", "American Samoa",       "+1",    "🇦🇸", 10, 10),

        // ── Special / territory ───────────────────────────────────────────────
        Country("RE", "Réunion",              "+262",  "🇷🇪",  9,  9),
        Country("YT", "Mayotte",              "+262",  "🇾🇹",  9,  9),
        Country("TF", "French Southern Territories","+262","🇹🇫",9, 9),
        Country("SH", "Saint Helena",         "+290",  "🇸🇭",  4,  4),
        Country("FK", "Falkland Islands",     "+500",  "🇫🇰",  5,  5),
        Country("GI", "Gibraltar",            "+350",  "🇬🇮",  8,  8),
        Country("GG", "Guernsey",             "+44",   "🇬🇬", 10, 10),
        Country("JE", "Jersey",               "+44",   "🇯🇪", 10, 10),
        Country("IM", "Isle of Man",          "+44",   "🇮🇲", 10, 10),
        Country("AX", "Åland Islands",        "+358",  "🇦🇽",  9, 10),
        Country("FO", "Faroe Islands",        "+298",  "🇫🇴",  6,  6),
        Country("GL", "Greenland",             "+299", "🇬🇱",  6,  6),
        Country("SJ", "Svalbard & Jan Mayen", "+47",   "🇸🇯",  8,  8),
        Country("BL", "Saint Barthélemy",     "+590",  "🇧🇱",  9,  9),
        Country("AI", "Anguilla",             "+1",    "🇦🇮", 10, 10),
        Country("MS", "Montserrat",           "+1",    "🇲🇸", 10, 10),
        Country("IO", "British Indian Ocean Territory","+246","🇮🇴",7,7),
        Country("CC", "Cocos Islands",        "+61",   "🇨🇨",  9,  9),
        Country("CX", "Christmas Island",     "+61",   "🇨🇽",  9,  9),
        Country("NF", "Norfolk Island",       "+672",  "🇳🇫",  6,  6),
        Country("PN", "Pitcairn Islands",     "+64",   "🇵🇳",  8,  9),
        Country("EH", "Western Sahara",       "+212",  "🇪🇭",  9,  9),

        // ── Additional African/island nations ─────────────────────────────────
        Country("MK", "North Macedonia",      "+389",  "🇲🇰",  8,  8),
        Country("TL", "Timor-Leste",          "+670",  "🇹🇱",  7,  8),
        Country("BN", "Brunei",               "+673",  "🇧🇳",  7,  7),
        Country("MU", "Mauritius",            "+230",  "🇲🇺",  7,  8),
        Country("DJ", "Djibouti",             "+253",  "🇩🇯",  8,  8)
    )
}
