package gizz.tapes.lastfm

import arrow.core.Either
import arrow.core.right
import gizz.tapes.Platform

class FakeLastFmApiClient : LastFmApiClient {
    val scrobbleCalls = mutableListOf<List<LastFmScrobble>>()
    val nowPlayingCalls = mutableListOf<LastFmTrack>()
    var scrobbleResponse: (List<LastFmScrobble>) -> Either<Exception, ScrobbleResult> =
        { ScrobbleResult(accepted = it.size, ignored = 0).right() }

    override fun authUrl(token: String, callback: String?) = "https://last.fm/auth?token=$token"
    override suspend fun getToken(): Either<Exception, String> = "token".right()
    override suspend fun getSession(token: String): Either<Exception, LastFmSession> = LastFmSession(
        "user",
        "sk"
    ).right()

    override suspend fun updateNowPlaying(sessionKey: String, track: LastFmTrack): Either<Exception, Unit> {
        nowPlayingCalls += track
        return Unit.right()
    }

    override suspend fun scrobble(sessionKey: String, scrobbles: List<LastFmScrobble>): Either<Exception, ScrobbleResult> {
        scrobbleCalls += scrobbles
        return scrobbleResponse(scrobbles)
    }
}

class InMemoryLastFmSessionStore(private var session: LastFmSession? = null) : LastFmSessionStore {
    override fun load() = session
    override fun save(session: LastFmSession) {
        this.session = session
    }
    override fun clear() {
        session = null
    }
}

fun fakeLastFmAuthManager(
    api: LastFmApiClient = FakeLastFmApiClient(),
    session: LastFmSession? = null,
) = LastFmAuthManager(api, InMemoryLastFmSessionStore(session), Platform.Desktop)
