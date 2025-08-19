package com.bryanguerra.dynamicislandmusic.di

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import com.bryanguerra.dynamicislandmusic.data.settings.SettingsRepository
import com.bryanguerra.dynamicislandmusic.data.visibility.UsageStatsRepository
import com.bryanguerra.dynamicislandmusic.overlay.OverlayWindowManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideSettingsRepository(@ApplicationContext ctx: Context) =
        SettingsRepository(ctx)

    @Provides @Singleton
    fun provideUsageStatsRepository(@ApplicationContext ctx: Context) =
        UsageStatsRepository(ctx)

    @Provides @Singleton
    fun provideKeyguardManager(@ApplicationContext ctx: Context): KeyguardManager =
        ctx.getSystemService(KeyguardManager::class.java)

    @Provides @Singleton
    fun provideNotificationManager(@ApplicationContext ctx: Context): NotificationManager =
        ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // OK con ApplicationContext para overlays tipo TYPE_APPLICATION_OVERLAY
    @Provides @Singleton
    fun provideOverlayWindowManager(@ApplicationContext ctx: Context) =
        OverlayWindowManager(ctx)
}
