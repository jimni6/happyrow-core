package com.happyrow.core.extension

fun <A, B> A.and(subject: B) = let { subject }

inline fun <T, R> T.then(block: T.() -> R): R = run { block() }

inline fun <F, S, R> Pair<F, S>.then(block: (Pair<F, S>) -> R): Pair<F, S> = also { block(it) }
