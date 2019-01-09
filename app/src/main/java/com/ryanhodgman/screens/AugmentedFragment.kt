package com.ryanhodgman.screens

import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.PointCloud
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.ux.ArFragment
import com.ryanhodgman.R
import com.ryanhodgman.ar.calcPolygonalArea
import com.ryanhodgman.ar.data.ExperimentDataRepository
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

    private lateinit var experimentDataRepo: ExperimentDataRepository

    private val pointCloudNode = PointCloudNode()

    private var experimentData = ExperimentDataRepository.ExperimentData()
    private var updateHandler: Handler? = null

    private var timeExperimentStartedMs = System.currentTimeMillis()
    private var timePlaneDetectedMs = -1L
    private var numFeaturesTracked = 0
    private var avgFeatureConfidence = 0f
    private var sizeLargestDetectedPlaneMetresSquared = 0.00

    //region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        job = Job()
        launch { pointCloudNode.prepare(context!!) }
        experimentDataRepo = ExperimentDataRepository(context!!)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = super.onCreateView(inflater, container, savedInstanceState)!! as ViewGroup
        arSceneView.scene.addChild(pointCloudNode)
        // TODO: Disabled for data collection. Re-enable for animation research
        // setupCustomPlaneDetectionGuidance(inflater, rootView)
        inflater.inflate(R.layout.layout_stabilisation_data, rootView, true)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btn_start_experiment.setOnClickListener {
            btn_start_experiment.visibility = View.GONE
            timeExperimentStartedMs = System.currentTimeMillis()
            btn_finish_experiment.visibility = View.VISIBLE
            updateHandler = Handler()
            appendCurrentRecord(experimentData)
        }
        btn_finish_experiment.setOnClickListener {
            experimentDataRepo.storeExperimentData(experimentData)
            startActivity(AugmentedActivity.newIntent(context!!))
            activity!!.finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        updateHandler = null
    }
    //endregion

    //region BaseArFragment
    override fun onUpdate(frameTime: FrameTime?) {
        super.onUpdate(frameTime)
        updatePointCloudData(arSceneView.arFrame.acquirePointCloud())
        arSceneView.arFrame?.let { updateFrameData(it) }
    }
    //endregion

    //region Internal methods
    private fun appendCurrentRecord(data: ExperimentDataRepository.ExperimentData) {
        val timeSinceExperimentStarted = System.currentTimeMillis() - timeExperimentStartedMs
        val record = ExperimentDataRepository.ExperimentData.Record(timeMs = timeSinceExperimentStarted,
                numFeaturesTracked = numFeaturesTracked, avgFeatureConfidence = avgFeatureConfidence,
                planeSizeMetres = sizeLargestDetectedPlaneMetresSquared)
        data.addRecord(record)
        updateHandler?.postDelayed({
            appendCurrentRecord(data)
        }, 100)
    }

    private fun updatePointCloudData(pointCloud: PointCloud) {
        pointCloudNode.update(pointCloud)
        numFeaturesTracked = pointCloud.numFeatures
        txt_num_points.text = "Number of feature points: $numFeaturesTracked"
        avgFeatureConfidence = if (pointCloud.numFeatures > 0) {
            val totalConfidence = pointCloud.features.fold(0f) { accum, element ->
                accum + element.confidence
            }
            totalConfidence / pointCloud.numFeatures
        } else 0f
        txt_avg_confidence.text = "Average feature point confidence: $avgFeatureConfidence%"
        // Surrender the point cloud's resources
        pointCloud.release()
    }

    private fun updateFrameData(frame: Frame) {
        val trackedPlanes = frame.getUpdatedTrackables(Plane::class.java)
        trackedPlanes.forEach { plane ->
            if (plane.trackingState == TrackingState.TRACKING) {
                if (timePlaneDetectedMs == -1L) {
                    timePlaneDetectedMs = System.currentTimeMillis()
                    txt_time_plane_detection.text = "Time of first plane detection: ${timePlaneDetectedMs - timeExperimentStartedMs}ms"
                }
                plane.calcPolygonalArea().let { area ->
                    if (area > sizeLargestDetectedPlaneMetresSquared) {
                        sizeLargestDetectedPlaneMetresSquared = area
                        txt_plane_size.text = "Size of detected plane: %.2fm^2".format(area)
                    }
                }
            }
        }
    }

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