/**
 * Provides pre-defined warning message templates and formats them
 * with domain/verdict data and the app download link.
 */
package com.safeanot.app.util

import com.safeanot.app.domain.model.WarningTemplate
import com.safeanot.app.domain.model.WarningTone

object WarningTemplateProvider {

    private val templates = listOf(
        WarningTemplate(
            id = "polite",
            tone = WarningTone.POLITE,
            templateEn = "Hi, I checked this link ({domain}) and it appears to be {verdict}. Please don't click it. You can check suspicious links with Safe Anot?",
            templateMs = "Hai, saya semak pautan ini ({domain}) dan ia kelihatan {verdict}. Tolong jangan klik. Anda boleh semak pautan mencurigakan dengan Safe Anot?",
        ),
        WarningTemplate(
            id = "urgent",
            tone = WarningTone.URGENT,
            templateEn = "WARNING: {domain} is a known scam! Do not click or enter any information. Check links with Safe Anot?",
            templateMs = "AMARAN: {domain} adalah penipuan! Jangan klik atau masukkan sebarang maklumat. Semak pautan dengan Safe Anot?",
        ),
        WarningTemplate(
            id = "elder_friendly",
            tone = WarningTone.ELDER_FRIENDLY,
            templateEn = "This link ({domain}) is not safe. Please delete it. If you clicked it, contact your bank immediately. Check links here:",
            templateMs = "Pautan ini ({domain}) tidak selamat. Sila padamkan. Jika anda telah klik, hubungi bank anda segera. Semak pautan di sini:",
        ),
    )

    fun getTemplates(): List<WarningTemplate> = templates

    /**
     * Replaces {domain} and {verdict} placeholders in the template text
     * for the given locale and appends the app download URL.
     *
     * @param template The warning template to format.
     * @param domain The domain that was checked.
     * @param verdict The verdict string (e.g. "DANGEROUS").
     * @param locale Language tag — "ms" for Bahasa Malaysia, defaults to English.
     * @return Fully formatted warning message with download link.
     */
    fun format(
        template: WarningTemplate,
        domain: String,
        verdict: String,
        locale: String = "en",
    ): String {
        val text = if (locale.startsWith("ms")) {
            template.templateMs
        } else {
            template.templateEn
        }

        return text
            .replace("{domain}", domain)
            .replace("{verdict}", verdict) +
            " ${Constants.APP_DOWNLOAD_URL}"
    }
}
