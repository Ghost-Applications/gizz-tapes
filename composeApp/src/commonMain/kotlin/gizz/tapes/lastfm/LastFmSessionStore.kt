package gizz.tapes.lastfm

import gizz.tapes.AppContext

/** Persists the user's Last.fm session key in platform secure storage. */
interface LastFmSessionStore {
    fun load(): LastFmSession?
    fun save(session: LastFmSession)
    fun clear()
}

expect fun createLastFmSessionStore(appContext: AppContext): LastFmSessionStore
