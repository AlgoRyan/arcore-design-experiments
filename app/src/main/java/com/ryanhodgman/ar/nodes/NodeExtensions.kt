package com.ryanhodgman.ar.nodes

import android.animation.ObjectAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.math.Vector3Evaluator

private const val ANIM_DURATION_MS = 1000L

/**
 * Runs a default animation that can be used to introduce new AR elements.
 *
 * @param targetScale The desired final scale of the [Node] after the animation.
 */
fun Node.playEntryAnimation(targetScale: Vector3) {
    ObjectAnimator().apply {
        propertyName = "localScale"
        setObjectValues(Vector3(0f, 0f, 0f), targetScale)
        setEvaluator(Vector3Evaluator())
        interpolator = AccelerateDecelerateInterpolator()
    }.also {
        it.target = this
        it.duration = ANIM_DURATION_MS
        it.start()
    }
}

/**
 * Runs a default animation that can be used to remove AR elements.
 */
fun Node.playExitAnimation() {
    ObjectAnimator().apply {
        propertyName = "localScale"
        setObjectValues(localScale, Vector3(0f, 0f, 0f))
        setEvaluator(Vector3Evaluator())
        interpolator = AccelerateDecelerateInterpolator()
    }.also {
        it.target = this
        it.duration = ANIM_DURATION_MS
        it.start()
    }
}