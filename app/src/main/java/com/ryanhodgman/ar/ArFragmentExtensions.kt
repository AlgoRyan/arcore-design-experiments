package com.ryanhodgman.ar

import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ux.ArFragment

/**
 * Allows for the simple detection of plane tap events.
 *
 * @author Ryan Hodgman
 */
fun ArFragment.setPlaneTappedListener(onPlaneTapped: (planeAnchor: AnchorNode) -> Unit) {
    setOnTapArPlaneListener { hitResult, _, _ ->
        val anchor = hitResult.createAnchor()
        val anchorNode = AnchorNode(anchor)
        anchorNode.setParent(arSceneView.scene)
        onPlaneTapped(anchorNode)
    }
}