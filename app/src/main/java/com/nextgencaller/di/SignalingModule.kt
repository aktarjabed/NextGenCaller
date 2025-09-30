package com.nextgencaller.di

import com.nextgencaller.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.socket.client.IO
import io.socket.client.Socket
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SignalingModule {

    @Provides
    @Singleton
    fun provideSocket(): Socket {
        return try {
            val opts = IO.Options()
            opts.forceNew = true
            opts.reconnection = true
            // This assumes the server supports the default EIEIO 3 protocol.
            // For newer servers, you might need to configure this.
            // opts.transports = arrayOf(Polling.NAME, WebSocket.NAME)
            IO.socket(BuildConfig.SIGNALING_SERVER_URL, opts)
        } catch (e: Exception) {
            throw RuntimeException("Failed to initialize Socket.IO", e)
        }
    }
}