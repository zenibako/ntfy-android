package io.heckel.ntfy.util

/**
 * The link an alert's "Open" should follow: the message's Click URL when it is a usable link
 * (`scheme:` followed by something, e.g. `hermes://chat/lead-dev/api_1` or `https://…`), else
 * null to open the topic instead.
 */
fun clickTarget(click: String): String? {
    val c = click.trim()
    return c.takeIf { Regex("""^[A-Za-z][A-Za-z0-9+.-]*:\S+$""").matches(it) }
}
