package com.ryanhodgman.screens

import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.ux.ArFragment
import com.ryanhodgman.R
import com.ryanhodgman.ar.features
import com.ryanhodgman.ar.nodes.PointCloudNode
import com.ryanhodgman.ar.numFeatures
import kotlinx.android.synthetic.main.layout_stabilisation_data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * @author Ryan Hodgman
 */
class AugmentedFragment : ArFragment(), CoroutineScope {

    //region CoroutineScope
    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main
    //endregion

    private val pointCloudNode = PointCloudNode()

    //region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        job = Job()
        launch { pointCloudNode.prepare(context!!) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = super.onCreateView(inflater, container, savedInstanceState)!! as ViewGroup
        arSceneView.scene.addChild(pointCloudNode)
        setupCustomPlaneDetectionGuidance(inflater, rootView)
        inflater.inflate(R.layout.layout_stabilisation_data, rootView, true)
        return rootView
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
    //endregion

    //region BaseArFragment
    override fun onUpdate(frameTime: FrameTime?) {
        super.onUpdate(frameTime)
        val pointCloud = arSceneView.arFrame.acquirePointCloud()
        pointCloudNode.update(pointCloud)
        txt_num_points.text = "Number of feature points: ${pointCloud.numFeatures}"
        val totalConfidence = pointCloud.features.fold(0f) { accum, element ->
            accum + element.confidence
        }
        val avgConfidence = totalConfidence / pointCloud.numFeatures
        txt_avg_confidence.text = "Average feature point confidence: $avgConfidence%"
        // Surrender the point cloud's resources
        pointCloud.release()
    }
    //endregion

    //region Internal methods
    private fun setupCustomPlaneDetectionGuidance(inflater: LayoutInflater, rootView: ViewGroup) {
        // Workaround for Sceneform issue - https://github.com/google-ar/sceneform-android-sdk/issues/179
        val handLayout = rootView.findViewById<FrameLayout>(R.id.sceneform_hand_layout)
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
    }
    //endregion
}