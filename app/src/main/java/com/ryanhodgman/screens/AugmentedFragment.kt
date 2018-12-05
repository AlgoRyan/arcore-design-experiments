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
import com.google.ar.sceneform.ux.ArFragment
import com.ryanhodgman.R
import com.ryanhodgman.ar.nodes.GamePieceNode
import com.ryanhodgman.ar.setPlaneTappedListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * @author Ryan Hodgman
 */
class AugmentedFragment : ArFragment(), CoroutineScope {

    private lateinit var gamePieceNode : GamePieceNode

    //region CoroutineScope
    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main
    //endregion

    //region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        job = Job()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val sceneformFrame = super.onCreateView(inflater, container, savedInstanceState)!!
        gamePieceNode = GamePieceNode(transformationSystem)
        gamePieceNode.prepare(context!!)
        setPlaneTappedListener { tapLocation ->
            tapLocation.addChild(gamePieceNode)
        }
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
}