package com.bryanguerra.dynamicislandmusic.di

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import com.bryanguerra.dynamicislandmusic.data.settings.SettingsRepository
import com.bryanguerra.dynamicislandmusic.data.visibility.UsageStatsRepository
import com.bryanguerra.dynamicislandmusic.domain.overlay.HideIslandUseCase
import com.bryanguerra.dynamicislandmusic.domain.overlay.OverlayRepository
import com.bryanguerra.dynamicislandmusic.domain.overlay.ShowIslandUseCase
import com.bryanguerra.dynamicislandmusic.overlay.OverlayWindowManager
import com.bryanguerra.dynamicislandmusic.presentation.navigation.IslandNavigator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.bryanguerra.dynamicislandmusic.data.overlay.OverlayRepositoryImplementation
import com.bryanguerra.dynamicislandmusic.presentation.navigation.IslandNavigatorImplementation

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext ctx: Context) = SettingsRepository(ctx)

    @Provides
    @Singleton
    fun provideUsageStatsRepository(@ApplicationContext ctx: Context) = UsageStatsRepository(ctx)

    @Provides
    @Singleton
    fun provideKeyguardManager(@ApplicationContext ctx: Context): KeyguardManager =
        ctx.getSystemService(KeyguardManager::class.java)

    @Provides
    @Singleton
    fun provideNotificationManager(@ApplicationContext ctx: Context): NotificationManager =
        ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // OK con ApplicationContext para overlays tipo TYPE_APPLICATION_OVERLAY
    @Provides
    @Singleton
    fun provideOverlayWindowManager(
        @ApplicationContext ctx: Context,
        islandNavigator: IslandNavigator
    ): OverlayWindowManager = OverlayWindowManager(ctx, islandNavigator)

    @Provides
    @Singleton
    fun provideOverlayRepository(overlayWindowManager: OverlayWindowManager): OverlayRepository =
        OverlayRepositoryImplementation(overlayWindowManager)

    @Provides
    @Singleton
    fun provideShowIslandUseCase(overlayRepository: OverlayRepository): ShowIslandUseCase =
        ShowIslandUseCase(overlayRepository)

    @Provides
    @Singleton
    fun provideHideIslandUseCase(overlayRepository: OverlayRepository): HideIslandUseCase =
        HideIslandUseCase(overlayRepository)

    @Provides
    @Singleton
    fun provideIslandNavigator(@ApplicationContext ctx: Context): IslandNavigator =
        IslandNavigatorImplementation(ctx)

    // Note: The `init` method and the lazy initialized properties like `overlayWM`, `overlayRepo`,
    // `showIslandUseCase`, `hideIslandUseCase`, and `islandNavigator` that were previously present
    // are generally not conventional for Hilt modules. Hilt manages the lifecycle and instantiation
    // of dependencies. The `init` method to set `appContext` is also redundant as Hilt provides
    // `@ApplicationContext`. The corrected version uses standard Hilt `@Provides` methods.
}
