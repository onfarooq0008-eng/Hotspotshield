package com.easyvpn.app.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.easyvpn.app.R
import com.easyvpn.app.data.AppSettings
import com.easyvpn.app.data.DnsProvider
import com.easyvpn.app.databinding.ActivitySettingsBinding

/** Regular-user settings only. Admin-only actions (change admin password, server
 *  setup info) live in AdminPanelActivity, reachable only after the hidden
 *  long-press + password login -- they must never appear here where any user
 *  of the published app would see them. */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var settings: AppSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settings = AppSettings(this)

        binding.switchAutoConnect.isChecked = settings.autoConnectEnabled
        binding.switchAutoConnect.setOnCheckedChangeListener { _, checked ->
            settings.autoConnectEnabled = checked
        }

        binding.switchKillSwitch.isChecked = settings.killSwitchEnabled
        binding.switchKillSwitch.setOnCheckedChangeListener { _, checked ->
            settings.killSwitchEnabled = checked
            if (checked) {
                Toast.makeText(
                    this,
                    "For a true system-level kill switch, also turn on \"Block connections without VPN\" for EasyVPN in system VPN settings.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        // Android only lets the *user* (not the app itself) turn on true lockdown
        // mode; that's deliberate OS security design, not a library limitation.
        // We deep-link straight to the right screen to make it a two-tap job.
        binding.buttonSystemVpnSettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_VPN_SETTINGS))
        }

        setUpThemeRadioGroup()
        setUpDnsSelector()

        binding.buttonSplitTunneling.setOnClickListener {
            startActivity(Intent(this, SplitTunnelActivity::class.java))
        }

        binding.buttonDiagnostics.setOnClickListener {
            startActivity(Intent(this, DiagnosticsActivity::class.java))
        }
    }

    private fun setUpThemeRadioGroup() {
        val checkedId = when (settings.themeMode) {
            "light" -> R.id.radioThemeLight
            "dark" -> R.id.radioThemeDark
            else -> R.id.radioThemeSystem
        }
        binding.radioGroupTheme.check(checkedId)

        binding.radioGroupTheme.setOnCheckedChangeListener { _, checkedButtonId ->
            val (mode, nightMode) = when (checkedButtonId) {
                R.id.radioThemeLight -> "light" to AppCompatDelegate.MODE_NIGHT_NO
                R.id.radioThemeDark -> "dark" to AppCompatDelegate.MODE_NIGHT_YES
                else -> "system" to AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            settings.themeMode = mode
            AppCompatDelegate.setDefaultNightMode(nightMode)
        }
    }

    /** Three DNS presets (see DnsProvider) shown as manually-exclusive rows --
     *  not a real RadioGroup, since each row needs its own two-line label and
     *  RadioGroup only manages direct RadioButton children. */
    private fun setUpDnsSelector() {
        val rows = listOf(
            Triple(DnsProvider.CLOUDFLARE, binding.rowDnsCloudflare, binding.radioDnsCloudflare),
            Triple(DnsProvider.GOOGLE, binding.rowDnsGoogle, binding.radioDnsGoogle),
            Triple(DnsProvider.ADBLOCKER, binding.rowDnsAdblock, binding.radioDnsAdblock)
        )

        fun select(chosen: DnsProvider) {
            rows.forEach { (provider, _, radio) -> radio.isChecked = provider == chosen }
        }

        select(DnsProvider.fromId(settings.dnsProviderId))

        rows.forEach { (provider, row, _) ->
            row.setOnClickListener {
                settings.dnsProviderId = provider.id
                select(provider)
            }
        }
    }
}
