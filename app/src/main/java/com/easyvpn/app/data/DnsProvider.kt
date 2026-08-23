package com.easyvpn.app.data

/**
 * DNS resolvers the user can pick between in Settings -> DNS. Whichever one is
 * selected overrides the server/backend-supplied DNS for every connection --
 * see MainActivity.doConnect() and AppSettings.dnsProviderId.
 */
enum class DnsProvider(
    val id: String,
    val displayName: String,
    val description: String,
    val primary: String,
    val secondary: String
) {
    CLOUDFLARE(
        id = "cloudflare",
        displayName = "Cloudflare",
        description = "Fast, privacy-focused resolver",
        primary = "1.1.1.1",
        secondary = "1.0.0.1"
    ),
    GOOGLE(
        id = "google",
        displayName = "Google",
        description = "Reliable, widely used resolver",
        primary = "8.8.8.8",
        secondary = "8.8.4.4"
    ),
    ADBLOCKER(
        id = "adblocker",
        displayName = "AdBlocker DNS",
        description = "Blocks ads & trackers at the DNS level",
        primary = "94.140.14.14",
        secondary = "94.140.15.15"
    );

    /** Comma-separated, ready for Interface.Builder.parseDnsServers(). */
    val addresses: String get() = "$primary, $secondary"

    companion object {
        val DEFAULT = CLOUDFLARE

        fun fromId(id: String?): DnsProvider = values().find { it.id == id } ?: DEFAULT
    }
}
