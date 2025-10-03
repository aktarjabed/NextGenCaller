package com.nextgencaller.data.remote

import android.content.Context
import com.nextgencaller.BuildConfig
import com.nextgencaller.domain.model.ConnectionQuality
import com.nextgencaller.domain.model.QualityMetrics
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
    private val peerConnectionFactory: PeerConnectionFactory,
    private val callQualityMonitor: CallQualityMonitor
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

    val qualityMetrics: StateFlow<QualityMetrics> = callQualityMonitor.qualityMetrics
    val connectionQuality: StateFlow<ConnectionQuality> = callQualityMonitor.connectionQuality

    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder(BuildConfig.TURN_SERVER_URL)
            .setUsername(BuildConfig.TURN_USERNAME)
            .setPassword(BuildConfig.TURN_CREDENTIAL)
            .createIceServer()
    )

    fun initializePeerConnection(
        onIceCandidate: (IceCandidate) -> Unit,
        onAddStream: (MediaStream) -> Unit,
        onConnectionChange: (PeerConnection.IceConnectionState) -> Unit = {}
    ) {
        close() // Ensure any previous connection is closed

        try {
            val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
                bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
                rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
                continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
                keyType = PeerConnection.KeyType.ECDSA
                sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
                enableDtlsSrtp = true
            }

            peerConnection = peerConnectionFactory.createPeerConnection(
                rtcConfig,
                object : PeerConnection.Observer {
                    override fun onIceCandidate(candidate: IceCandidate) {
                        Timber.d("🧊 New ICE candidate")
                        onIceCandidate(candidate)
                    }

                    override fun onAddStream(stream: MediaStream) {
                        Timber.d("📺 Remote stream added")
                        _remoteStream.value = stream
                        onAddStream(stream)
                    }

                    override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                        Timber.d("🌐 ICE connection state: $state")
                        _iceConnectionState.value = state
                        onConnectionChange(state)
                        if (state == PeerConnection.IceConnectionState.CONNECTED) {
                            peerConnection?.let { callQualityMonitor.startMonitoring(it) }
                        } else if (state == PeerConnection.IceConnectionState.DISCONNECTED || state == PeerConnection.IceConnectionState.FAILED) {
                            callQualityMonitor.stopMonitoring()
                        }
                    }

                    override fun onSignalingChange(state: PeerConnection.SignalingState) {
                        Timber.d("📡 Signaling state: $state")
                    }
                    override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
                    override fun onRemoveStream(stream: MediaStream?) {}
                    override fun onIceConnectionReceivingChange(receiving: Boolean) {}
                    override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {}
                    override fun onRenegotiationNeeded() {}
                    override fun onDataChannel(dataChannel: DataChannel?) {}
                    override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}
                }
            )

            Timber.d("✅ Peer connection initialized")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to initialize peer connection")
        }
    }

    fun startLocalStream(isVideoEnabled: Boolean = true) {
        try {
            val audioConstraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
            }
            audioSource = peerConnectionFactory.createAudioSource(audioConstraints)
            localAudioTrack = peerConnectionFactory.createAudioTrack("audio_track", audioSource)

            if (isVideoEnabled) {
                videoCapturer = createCameraCapturer()
                videoCapturer?.let { capturer ->
                    surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", EglBase.create().eglBaseContext)
                    videoSource = peerConnectionFactory.createVideoSource(capturer.isScreencast)
                    capturer.initialize(surfaceTextureHelper, context, videoSource?.capturerObserver)
                    capturer.startCapture(1280, 720, 30)
                    localVideoTrack = peerConnectionFactory.createVideoTrack("video_track", videoSource)
                }
            }

            val localMediaStream = peerConnectionFactory.createLocalMediaStream("local_stream")
            localAudioTrack?.let { localMediaStream.addTrack(it) }
            localVideoTrack?.let { localMediaStream.addTrack(it) }

            peerConnection?.addStream(localMediaStream)
            _localStream.value = localMediaStream

            Timber.d("✅ Local stream started")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to start local stream")
        }
    }

    private fun createCameraCapturer(): VideoCapturer? {
        return try {
            val enumerator = Camera2Enumerator(context)
            val deviceName = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
                ?: enumerator.deviceNames.firstOrNull()

            if (deviceName != null) {
                enumerator.createCapturer(deviceName, null)
            } else {
                Timber.e("❌ No camera found")
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to create camera capturer")
            null
        }
    }

    suspend fun createOffer(): SessionDescription? = suspendCancellableCoroutine { cont ->
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
        peerConnection?.createOffer(sdpObserver(cont, "offer"), constraints)
    }

    suspend fun createAnswer(): SessionDescription? = suspendCancellableCoroutine { cont ->
        peerConnection?.createAnswer(sdpObserver(cont, "answer"), MediaConstraints())
    }

    private fun sdpObserver(continuation: CancellableContinuation<SessionDescription?>, type: String) = object : SdpObserver {
        override fun onCreateSuccess(sdp: SessionDescription?) {
            if (sdp == null) {
                continuation.resumeWithException(Exception("SDP is null"))
                return
            }
            peerConnection?.setLocalDescription(object : SdpObserver {
                override fun onSetSuccess() {
                    Timber.d("✅ Local description set ($type)")
                    continuation.resume(sdp)
                }
                override fun onSetFailure(error: String?) {
                    Timber.e("❌ Set local description failed: $error")
                    continuation.resumeWithException(Exception(error))
                }
                override fun onCreateSuccess(p0: SessionDescription?) {}
                override fun onCreateFailure(p0: String?) {}
            }, sdp)
        }
        override fun onCreateFailure(error: String?) {
            Timber.e("❌ Create $type failed: $error")
            continuation.resumeWithException(Exception(error))
        }
        override fun onSetSuccess() {}
        override fun onSetFailure(p0: String?) {}
    }

    fun setRemoteDescription(sdp: SessionDescription) {
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() { Timber.d("✅ Remote description set") }
            override fun onSetFailure(error: String?) { Timber.e("❌ Set remote description failed: $error") }
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(p0: String?) {}
        }, sdp)
    }

    fun addIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
    }

    fun toggleAudio(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
    }

    fun toggleVideo(enabled: Boolean) {
        localVideoTrack?.setEnabled(enabled)
    }

    fun switchCamera() {
        (videoCapturer as? CameraVideoCapturer)?.switchCamera(null)
    }

    fun close() {
        try {
            callQualityMonitor.stopMonitoring()
            videoCapturer?.stopCapture()
            surfaceTextureHelper?.stopListening()
            localVideoTrack?.dispose()
            videoCapturer?.dispose()
            videoSource?.dispose()
            surfaceTextureHelper?.dispose()
            localAudioTrack?.dispose()
            audioSource?.dispose()
            peerConnection?.close()
            peerConnection?.dispose()
        } catch (e: Exception) {
            Timber.e(e, "❌ Error closing WebRTC client")
        } finally {
            peerConnection = null
            localVideoTrack = null
            localAudioTrack = null
            videoCapturer = null
            surfaceTextureHelper = null
            audioSource = null
            videoSource = null
            _localStream.value = null
            _remoteStream.value = null
            _iceConnectionState.value = null
            Timber.d("✅ WebRTC client resources released")
        }
    }
}