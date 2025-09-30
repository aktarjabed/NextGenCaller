package com.nextgencaller.data.remote

import com.google.gson.Gson
import io.socket.client.Socket
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignalingClient @Inject constructor(
    private val socket: Socket,
    private val gson: Gson
) {
    private var currentUserId: String? = null
    private var currentRoomId: String? = null

    init {
        setupSocketListeners()
    }

    private fun setupSocketListeners() {
        socket.on(Socket.EVENT_CONNECT) {
            Timber.d("✅ Connected to signaling server: ${socket.id()}")
            currentUserId?.let { register(it) }
        }

        socket.on(Socket.EVENT_DISCONNECT) {
            Timber.w("❌ Disconnected from signaling server")
        }

        socket.on(Socket.EVENT_CONNECT_ERROR) { args ->
            Timber.e("🔴 Connection error: ${args.firstOrNull()}")
        }

        socket.on(Socket.EVENT_RECONNECT) { args ->
            Timber.i("🔄 Reconnected after ${args.firstOrNull()} attempts")
            currentUserId?.let { register(it) }
            currentRoomId?.let { joinRoom(it) }
        }
    }

    fun connect(userId: String) {
        currentUserId = userId
        if (!socket.connected()) {
            socket.connect()
        } else {
            register(userId)
        }
    }

    fun disconnect() {
        currentRoomId?.let { leaveRoom(it) }
        socket.disconnect()
        currentUserId = null
    }

    private fun register(userId: String) {
        val data = JSONObject().apply {
            put("userId", userId)
        }
        socket.emit("register", data)
        Timber.d("📝 Registered with userId: $userId")
    }

    fun joinRoom(roomId: String) {
        currentRoomId = roomId
        val data = JSONObject().apply {
            put("roomId", roomId)
        }
        socket.emit("join-room", data)
        Timber.d("🚪 Joined room: $roomId")
    }

    fun leaveRoom(roomId: String) {
        val data = JSONObject().apply {
            put("roomId", roomId)
        }
        socket.emit("leave-room", data)
        currentRoomId = null
        Timber.d("🚪 Left room: $roomId")
    }

    fun observeIncomingOffers(): Flow<com.nextgencaller.domain.model.OfferSignal> = callbackFlow {
        val listener = { args: Array<Any> ->
            try {
                val data = args[0] as JSONObject
                val offer = com.nextgencaller.domain.model.OfferSignal(
                    fromUserId = data.getString("from"),
                    callerName = data.getString("callerName"),
                    isVideo = data.getBoolean("isVideo"),
                    sdp = data.getString("sdp"),
                    type = data.getString("type"),
                    roomId = data.optString("roomId", "")
                )
                trySend(offer).isSuccess
                Timber.d("📞 Received offer from ${offer.fromUserId}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse incoming offer")
            }
        }
        socket.on("offer", listener)
        awaitClose { socket.off("offer", listener) }
    }

    fun observeAnswers(): Flow<com.nextgencaller.domain.model.AnswerSignal> = callbackFlow {
        val listener = { args: Array<Any> ->
            try {
                val data = args[0] as JSONObject
                val answer = com.nextgencaller.domain.model.AnswerSignal(
                    fromUserId = data.getString("from"),
                    sdp = data.getString("sdp"),
                    type = data.getString("type")
                )
                trySend(answer).isSuccess
                Timber.d("✅ Received answer from ${answer.fromUserId}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse answer")
            }
        }
        socket.on("answer", listener)
        awaitClose { socket.off("answer", listener) }
    }

    fun observeIceCandidates(): Flow<com.nextgencaller.domain.model.IceCandidateSignal> = callbackFlow {
        val listener = { args: Array<Any> ->
            try {
                val data = args[0] as JSONObject
                val candidate = com.nextgencaller.domain.model.IceCandidateSignal(
                    fromUserId = data.getString("from"),
                    candidate = data.getString("candidate"),
                    sdpMid = data.getString("sdpMid"),
                    sdpMLineIndex = data.getInt("sdpMLineIndex")
                )
                trySend(candidate).isSuccess
                Timber.d("🧊 Received ICE candidate from ${candidate.fromUserId}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse ICE candidate")
            }
        }
        socket.on("ice-candidate", listener)
        awaitClose { socket.off("ice-candidate", listener) }
    }

    fun observeCallEnded(): Flow<String> = callbackFlow {
        val listener = { args: Array<Any> ->
            try {
                val data = args[0] as JSONObject
                val userId = data.getString("userId")
                trySend(userId).isSuccess
                Timber.d("☎️ Call ended by user: $userId")
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse call ended signal")
            }
        }
        socket.on("call-ended", listener)
        awaitClose { socket.off("call-ended", listener) }
    }

    fun sendOffer(toUserId: String, callerName: String, isVideo: Boolean, offer: SessionDescription) {
        val data = JSONObject().apply {
            put("to", toUserId)
            put("callerName", callerName)
            put("isVideo", isVideo)
            put("type", offer.type.canonicalForm())
            put("sdp", offer.description)
        }
        socket.emit("offer", data)
        Timber.d("📤 Sent offer to $toUserId")
    }

    fun sendAnswer(toUserId: String, answer: SessionDescription) {
        val data = JSONObject().apply {
            put("to", toUserId)
            put("type", answer.type.canonicalForm())
            put("sdp", answer.description)
        }
        socket.emit("answer", data)
        Timber.d("📤 Sent answer to $toUserId")
    }

    fun sendIceCandidate(toUserId: String, candidate: IceCandidate) {
        val data = JSONObject().apply {
            put("to", toUserId)
            put("candidate", candidate.sdp)
            put("sdpMid", candidate.sdpMid)
            put("sdpMLineIndex", candidate.sdpMLineIndex)
        }
        socket.emit("ice-candidate", data)
        Timber.d("📤 Sent ICE candidate to $toUserId")
    }

    fun endCall(toUserId: String) {
        val data = JSONObject().apply {
            put("to", toUserId)
        }
        socket.emit("end-call", data)
        Timber.d("☎️ Sent end call signal to $toUserId")
    }

    fun isConnected(): Boolean = socket.connected()
}