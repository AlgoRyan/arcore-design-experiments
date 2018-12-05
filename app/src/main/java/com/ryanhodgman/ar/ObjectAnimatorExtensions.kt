package com.ryanhodgman.ar

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun ObjectAnimator.startBlockingAnimation() {
    suspendCoroutine<Unit> { continuation ->
        start()
        doOnEnd { continuation.resume(Unit) }
    }
}

fun ObjectAnimator.doOnEnd(onEnd: () -> Unit) {
    addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
            onEnd()
        }
    })
}