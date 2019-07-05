package com.tunebrains.cpu.library

import android.content.Context
import com.tunebrains.cpu.library.cmd.*
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.mockito.runners.MockitoJUnitRunner
import java.io.File

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
@RunWith(MockitoJUnitRunner::class)
class DownloaderTest {
    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var api: IMedicaApi

    @Mock
    private lateinit var source: ISDKSource

    @Mock
    private lateinit var dbHelper: IDbHelper

    private fun <T> any(): T {
        Mockito.any<T>()
        return uninitialized()
    }

    private fun <T> uninitialized(): T = null as T
    @Test
    fun handlesError() {

        `when`(mockContext.cacheDir).thenReturn(File(""))

        val localCommand = LocalCommand(1, "", "", null, LocalCommandStatus.QUEUED)
        val localCommand2 = LocalCommand(2, "", "", null, LocalCommandStatus.QUEUED)

        `when`(api.downloadCommand(localCommand, File(""))).thenReturn(Single.error(NullPointerException()))

        val emitter = PublishSubject.create<LocalCommand>()

        `when`(source.observe()).thenReturn(emitter)

        val enqueuer = CommandDownloader(mockContext, api, source, dbHelper)
        enqueuer.start()

        emitter.onNext(localCommand)

        verify(api).downloadCommand(localCommand, File(""))

        emitter.onNext(localCommand2)

        verify(api).downloadCommand(localCommand2, File(""))

    }

    @Test
    fun filtersNotQUEUEDStatus() {

        `when`(mockContext.cacheDir).thenReturn(File(""))

        val localCommand = LocalCommand(2, "", "", null, LocalCommandStatus.EXECUTED)

        val emitter = PublishSubject.create<LocalCommand>()

        `when`(source.observe()).thenReturn(emitter)

        val enqueuer = CommandDownloader(mockContext, api, source, dbHelper)
        enqueuer.start()

        emitter.onNext(localCommand)

        verify(api, times(0)).downloadCommand(localCommand, File(""))
    }

    @Test
    fun changesStatusToDownloaded() {
        `when`(mockContext.cacheDir).thenReturn(File(""))
        val localCommand = LocalCommand(2, "2", "", null, LocalCommandStatus.QUEUED)
        val withServer = localCommand.withServer(
            ServerCommand(
                "2", "", "",
                emptyMap()
            )
        )
        `when`(api.downloadCommand(localCommand, File(""))).thenReturn(
            Single.just(
                withServer
            )
        )

        val emitter = PublishSubject.create<LocalCommand>()

        `when`(source.observe()).thenReturn(emitter)

        val enqueuer = CommandDownloader(mockContext, api, source, dbHelper)
        enqueuer.start()

        emitter.onNext(localCommand)

        verify(dbHelper, times(1)).commandDownloaded(withServer)
    }


}