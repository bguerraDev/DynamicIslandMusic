package com.bryanguerra.dynamicislandmusic.domain.visibility

import com.bryanguerra.dynamicislandmusic.data.visibility.UsageStatsRepository
import javax.inject.Inject

class IsTargetAppInForegroundUseCase @Inject constructor(
    private val repo: UsageStatsRepository
) {
    operator fun invoke(pkg: String) = repo.isAppInForeground(pkg)
}