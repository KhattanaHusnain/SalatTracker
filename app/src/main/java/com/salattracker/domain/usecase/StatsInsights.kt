package com.salattracker.domain.usecase

/** A deliberately small insight vocabulary keeps Stats explanatory without inventing weak claims. */
sealed interface StatsInsight {
    data class CompletionChanged(val points: Int) : StatsInsight
}

fun statsInsight(completionDelta: Int?): StatsInsight? =
    completionDelta?.takeIf { kotlin.math.abs(it) >= 10 }?.let(StatsInsight::CompletionChanged)
