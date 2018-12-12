package com.ryanhodgman.screens

import android.content.res.AssetManager
import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.ux.ArFragment
import com.ryanhodgman.R
import com.ryanhodgman.ar.nodes.SphereNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

/**
 * @author Ryan Hodgman
 */
class AugmentedFragment : ArFragment(), CoroutineScope {

    private companion object {
        private const val IMAGE_DATABASE = "imagetargets.imgdb"
    }

    //region CoroutineScope
    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main
    //endregion

    private val activelyTrackedTargets = mutableSetOf<AugmentedImage>()

    //region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        job = Job()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val sceneformFrame = super.onCreateView(inflater, container, savedInstanceState)!!
        // Workaround for Sceneform issue - https://github.com/google-ar/sceneform-android-sdk/issues/179
        val handLayout = sceneformFrame.findViewById<FrameLayout>(R.id.sceneform_hand_layout)
        handLayout.removeAllViews()
        inflater.inflate(R.layout.view_plane_discovery, handLayout, true)
            .findViewById<ImageView>(R.id.img_plane_discovery).let { imageView ->
                (imageView.drawable as AnimatedVectorDrawable).let { anim ->
                    anim.registerAnimationCallback(object : Animatable2.AnimationCallback() {
                        override fun onAnimationEnd(drawable: Drawable) {
                            imageView.post { anim.start() }
                        }
                    })
                    anim.start()
                }
                planeDiscoveryController.setInstructionView(imageView)
            }
        return sceneformFrame
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
    //endregion


    //region ArFragment
    override fun getSessionConfiguration(session: Session): Config =
        super.getSessionConfiguration(session).apply {
            augmentedImageDatabase = readImageDatabase(context!!.assets!!, session, IMAGE_DATABASE)
        }

    override fun onUpdate(frameTime: FrameTime?) {
        super.onUpdate(frameTime)
        arSceneView.arFrame?.let { frame ->
            // If ARCore is not tracking yet, just return
            if (frame.camera.trackingState != TrackingState.TRACKING) return
            for (augmentedImage in frame.getUpdatedTrackables(AugmentedImage::class.java)) {
                when (augmentedImage.trackingState!!) {
                    TrackingState.TRACKING -> {
                        if (!activelyTrackedTargets.contains(augmentedImage)) {
                            // Create a new anchor for newly found images
                            val anchor = augmentedImage.createAnchor(augmentedImage.centerPose)
                            AnchorNode(anchor).apply {
                                setParent(arSceneView.scene)
                                addChild(SphereNode(context!!))
                            }
                            activelyTrackedTargets.add(augmentedImage)
                        }
                    }
                    TrackingState.STOPPED -> activelyTrackedTargets.remove(augmentedImage)
                    TrackingState.PAUSED -> {
                        // When an image is in the PAUSED state, but the camera is not PAUSED,
                        // the image has been detected but is not yet tracked. No action required
                    }
                }
            }
        }
    }
    //endregion

    //region Internal methods
    private fun readImageDatabase(assetManager: AssetManager, session: Session,
                                  dbName: String): AugmentedImageDatabase =
        assetManager.open(dbName).use { AugmentedImageDatabase.deserialize(session, it) }
    //endregion
}