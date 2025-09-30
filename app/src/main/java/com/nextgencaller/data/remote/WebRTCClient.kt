package com.nextgencaller.data.remote

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.webrtc.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class WebRTCClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val peerConnectionFactory: PeerConnectionFactory
) {
    private var peerConnection: PeerConnection? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoCapturer: VideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var audioSource: AudioSource? = null
    private var videoSource: VideoSource? = null

    private val _localStream = MutableStateFlow<MediaStream?>(null)
    val localStream: StateFlow<MediaStream?> = _localStream.asStateFlow()

    private val _remoteStream = MutableStateFlow<MediaStream?>(null)
    val remoteStream: StateFlow<MediaStream?> = _remoteStream.asStateFlow()

    private val _iceConnectionState = MutableStateFlow<PeerConnection.IceConnectionState?>(null)
    val iceConnectionState: StateFlow<PeerConnection.IceConnectionState?> = _iceConnectionState.asStateFlow()

    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer()
    )

    fun initializePeerConnection(
        onIceCandidate: (IceCandidate) -> Unit,
        onAddStream: (MediaStream) -> Unit,
        onConnectionChange: (PeerConnection.IceConnectionState) -> Unit = {}
    ): PeerConnection? {
        try {
            val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
                bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
                rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
                tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
                continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
                keyType = PeerConnection.KeyType.ECDSA
                sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
                enableDtlsSrtp = true
            }

            peerConnection = peerConnectionFactory.createPeerConnection(
                rtcConfig,
                object : PeerConnection.Observer {
                    override fun onIceCandidate(candidate: IceCandidate) {
                        Timber.d("🧊 New ICE candidate: ${candidate.sdp}")
                        onIceCandidate(candidate)
                    }

                    override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
                        Timber.d("🗑️ ICE candidates removed: ${candidates?.size}")
                    }

                    override fun onAddStream(stream: MediaStream) {
                        Timber.d("📺 Remote stream added: audio=${stream.audioTracks.size}, video=${stream.videoTracks.size}")
                        _remoteStream.value = stream
                        onAddStream(stream)
                    }

                    override fun onRemoveStream(stream: MediaStream?) {
                        Timber.d("📺 Remote stream removed")
                        _remoteStream.value = null
                    }

                    override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                        Timber.d("🌐 ICE connection state: $state")
                        _iceConnectionState.value = state
                        onConnectionChange(state)
                    }

                    override fun onIceConnectionReceivingChange(receiving: Boolean) {
                        Timber.d("📡 ICE receiving: $receiving")
                    }

                    override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {
                        Timber.d("🧊 ICE gathering state: $state")
                    }

                    override fun onSignalingChange(state: PeerConnection.SignalingState) {
                        Timber.d("📡 Signaling state: $state")
                    }

                    override fun onRenegotiationNeeded() {
                        Timber.d("🔄 Renegotiation needed")
                    }

                    override fun onDataChannel(dataChannel: DataChannel?) {
                        Timber.d("📊 Data channel: ${dataChannel?.label()}")
                    }

                    override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                        Timber.d("🎵 Track added: ${receiver?.track()?.kind()}")
                    }
                }
            )

            Timber.d("✅ Peer connection initialized")
            return peerConnection
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to initialize peer connection")
            return null
        }
    }

    fun startLocalStream(isVideoEnabled: Boolean = true) {
        try {
            // Audio setup
            val audioConstraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
            }

            audioSource = peerConnectionFactory.createAudioSource(audioConstraints)
            localAudioTrack = peerConnectionFactory.createAudioTrack("audio_track", audioSource)

            // Video setup
            if (isVideoEnabled) {
                videoCapturer = createCameraCapturer()
                videoCapturer?.let { capturer ->
                    surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", null)
                    videoSource = peerConnectionFactory.createVideoSource(capturer.isScreencast)
                    capturer.initialize(surfaceTextureHelper, context, videoSource?.capturerObserver)
                    capturer.startCapture(1280, 720, 30)
                    localVideoTrack = peerConnectionFactory.createVideoTrack("video_track", videoSource)
                }
            }

            // Create local media stream
            val localMediaStream = peerConnectionFactory.createLocalMediaStream("local_stream")
            localAudioTrack?.let { localMediaStream.addTrack(it) }
            localVideoTrack?.let { localMediaStream.addTrack(it) }

            peerConnection?.addStream(localMediaStream)
            _localStream.value = localMediaStream

            Timber.d("✅ Local stream started: audio=${localAudioTrack != null}, video=${localVideoTrack != null}")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to start local stream")
        }
    }

    private fun createCameraCapturer(): VideoCapturer? {
        val enumerator = Camera2Enumerator(context)

        enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }?.let { deviceName ->
            return enumerator.createCapturer(deviceName, null)?.also {
                Timber.d("📷 Using front camera: $deviceName")
            }
        }

        enumerator.deviceNames.firstOrNull { enumerator.isBackFacing(it) }?.let { deviceName ->
            return enumerator.createCapturer(deviceName, null)?.also {
                Timber.d("📷 Using back camera: $deviceName")
            }
        }

        Timber.e("❌ No camera found")
        return null
    }

    suspend fun createOffer(): SessionDescription? {
        return suspendCancellableCoroutine { continuation ->
            val constraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            }

            peerConnection?.createOffer(object : SdpObserver {
                override fun onCreateSuccess(sdp: SessionDescription?) {
                    if (sdp == null) {
                        continuation.resumeWithException(Exception("SDP is null"))
                        return
                    }
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onSetSuccess() {
                            Timber.d("✅ Local description set (offer)")
                            continuation.resume(sdp)
                        }
                        override fun onSetFailure(error: String?) {
                            Timber.e("❌ Set local description failed: $error")
                            continuation.resumeWithException(Exception(error ?: "Unknown error"))
                        }
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onCreateFailure(p0: String?) {}
                    }, sdp)
                }
                override fun onCreateFailure(error: String?) {
                    Timber.e("❌ Create offer failed: $error")
                    continuation.resumeWithException(Exception(error ?: "Unknown error"))
                }
                override fun onSetSuccess() {}
                override fun onSetFailure(p0: String?) {}
            }, constraints)
        }
    }

    suspend fun createAnswer(): SessionDescription? {
        return suspendCancellableCoroutine { continuation ->
            val constraints = MediaConstraints()
            peerConnection?.createAnswer(object : SdpObserver {
                override fun onCreateSuccess(sdp: SessionDescription?) {
                    if (sdp == null) {
                        continuation.resumeWithException(Exception("SDP is null"))
                        return
                    }
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onSetSuccess() {
                            Timber.d("✅ Local description set (answer)")
                            continuation.resume(sdp)
                        }
                        override fun onSetFailure(error: String?) {
                            Timber.e("❌ Set local description failed: $error")
                            continuation.resumeWithException(Exception(error ?: "Unknown error"))
                        }
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onCreateFailure(p0: String?) {}
                    }, sdp)
                }
                override fun onCreateFailure(error: String?) {
                    Timber.e("❌ Create answer failed: $error")
                    continuation.resumeWithException(Exception(error ?: "Unknown error"))
                }
                override fun onSetSuccess() {}
                override fun onSetFailure(p0: String?) {}
            }, constraints)
        }
    }

    fun setRemoteDescription(sdp: SessionDescription) {
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                Timber.d("✅ Remote description set")
            }
            override fun onSetFailure(error: String?) {
                Timber.e("❌ Set remote description failed: $error")
            }
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(p0: String?) {}
        }, sdp)
    }

    fun addIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
    }

    fun toggleAudio(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
        Timber.d("🎤 Audio ${if (enabled) "enabled" else "disabled"}")
    }

    fun toggleVideo(enabled: Boolean) {
        localVideoTrack?.setEnabled(enabled)
        Timber.d("📹 Video ${if (enabled) "enabled" else "disabled"}")
    }

    fun switchCamera() {
        (videoCapturer as? CameraVideoCapturer)?.switchCamera(null)
        Timber.d("🔄 Camera switched")
    }

    fun close() {
        try {
            videoCapturer?.stopCapture()
            videoCapturer?.dispose()
            videoCapturer = null

            surfaceTextureHelper?.dispose()
            surfaceTextureHelper = null

            localVideoTrack?.dispose()
            localVideoTrack = null

            localAudioTrack?.dispose()
            localAudioTrack = null

            videoSource?.dispose()
            videoSource = null

            audioSource?.dispose()
            audioSource = null

            _localStream.value?.audioTracks?.forEach { it.dispose() }
            _localStream.value?.videoTracks?.forEach { it.dispose() }
            _localStream.value?.dispose()

            peerConnection?.close()
            peerConnection?.dispose()
            peerConnection = null

            _localStream.value = null
            _remoteStream.value = null
            _iceConnectionState.value = null

            Timber.d("✅ WebRTC client closed")
        } catch (e: Exception) {
            Timber.e(e, "❌ Error closing WebRTC client")
        }
    }
}