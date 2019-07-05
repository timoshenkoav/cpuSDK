package com.tunebrains.cpu.library

import android.content.Context
import com.tunebrains.cpu.library.cmd.*
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.runners.MockitoJUnitRunner

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
@RunWith(MockitoJUnitRunner::class)
class EnqueuerTest {
    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var api: IMedicaApi

    @Mock
    private lateinit var source: ISDKSource

    @Mock
    private lateinit var dbHelper: IDbHelper

    @Test
    fun enqueuerHandlesError() {
        val localCommand = LocalCommand(1, "", "", null, LocalCommandStatus.NONE)
        val localCommand2 = LocalCommand(2, "", "", null, LocalCommandStatus.NONE)

        `when`(api.command(localCommand)).thenReturn(Single.error(NullPointerException()))

        val emitter = PublishSubject.create<LocalCommand>()

        `when`(source.observe()).thenReturn(emitter)

        val enqueuer = CommandEnqueuer(mockContext, api, source, dbHelper)
        enqueuer.start()

        emitter.onNext(localCommand)

        verify(api).command(localCommand)

        emitter.onNext(localCommand2)

        verify(api).command(localCommand2)

    }

    @Test
    fun enqueuerFiltersNotNoneStatus() {

        val localCommand = LocalCommand(2, "", "", null, LocalCommandStatus.EXECUTED)

        val emitter = PublishSubject.create<LocalCommand>()

        `when`(source.observe()).thenReturn(emitter)

        val enqueuer = CommandEnqueuer(mockContext, api, source, dbHelper)
        enqueuer.start()

        emitter.onNext(localCommand)

        verify(api, times(0)).command(localCommand)
    }

    @Test
    fun enqueuerChangesStatusToEnqueued() {
        val localCommand = LocalCommand(2, "2", "", null, LocalCommandStatus.NONE)
        val withServer = localCommand.withServer(
            ServerCommand(
                "2", "", "",
                emptyMap()
            )
        )
        `when`(api.command(localCommand)).thenReturn(
            Single.just(
                withServer
            )
        )

        val emitter = PublishSubject.create<LocalCommand>()

        `when`(source.observe()).thenReturn(emitter)

        val enqueuer = CommandEnqueuer(mockContext, api, source, dbHelper)
        enqueuer.start()

        emitter.onNext(localCommand)

        verify(dbHelper, times(1)).commandEnqueud(withServer)
    }


}