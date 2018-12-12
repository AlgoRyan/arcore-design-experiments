package com.ryanhodgman.ar.nodes

import android.content.Context
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ShapeFactory

/**
 * @author Ryan Hodgman
 */
open class SphereNode(context: Context) : CoroutineNode() {

    init {
        MaterialFactory.makeOpaqueWithColor(context, Color()).thenAccept {
            renderable = ShapeFactory.makeSphere(0.2f, Vector3.zero(), it)
        }
    }
}