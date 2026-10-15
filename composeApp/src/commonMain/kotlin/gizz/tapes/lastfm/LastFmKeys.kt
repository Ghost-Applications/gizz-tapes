package gizz.tapes.lastfm

/**
 * Last.fm API credentials. Shipped in the app on purpose: any secret bundled in a client can be
 * extracted, and these only identify the app. Scrobbling still needs each user's own session key.
 */
object LastFmKeys {
    // From https://www.last.fm/api/accounts
    const val API_KEY = "3f15c7aad979f1e647554a558d2e2d27"
    const val SHARED_SECRET = "b67ec0043b5e61d860e6f751a0d3f1ba"
}
