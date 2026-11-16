package id.ac.um.mivvisecure

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

object AppPrefs {
    const val PREFS = "mivvi_settings"
    const val KEY_API_URL = "api_base_url"
    const val KEY_TRUSTED_SENDERS = "trusted_senders"
    const val KEY_INCLUDE_WHATSAPP = "include_whatsapp"
    const val KEY_INCLUDE_SMS = "include_sms"

    fun trustedSenders(context: Context): Set<String> = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(KEY_TRUSTED_SENDERS, "").orEmpty()
        .lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toSet()

    fun notificationAccessEnabled(context: Context): Boolean {
        val raw = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
        return raw.split(":").any { flat -> ComponentName.unflattenFromString(flat)?.packageName == context.packageName }
    }

    fun includeWhatsApp(context: Context): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getBoolean(KEY_INCLUDE_WHATSAPP, true)
    fun includeSms(context: Context): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getBoolean(KEY_INCLUDE_SMS, true)
}
