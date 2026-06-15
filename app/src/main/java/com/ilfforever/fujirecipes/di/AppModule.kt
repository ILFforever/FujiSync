package com.ilfforever.fujirecipes.di

import android.content.Context
import android.hardware.usb.UsbManager
import com.ilfforever.fujirecipes.data.local.LocalStore
import com.ilfforever.fujirecipes.BuildConfig
import com.ilfforever.fujirecipes.data.update.GitHubReleaseUpdater
import com.ilfforever.fujirecipes.data.usb.CameraHeartbeat
import com.ilfforever.fujirecipes.data.usb.UsbCameraRepository
import com.ilfforever.fujirecipes.data.usb.UsbCameraScanner
import com.ilfforever.fujirecipes.data.usb.UsbPtpConnection
import com.ilfforever.fujirecipes.domain.repository.CameraRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideUsbManager(@ApplicationContext context: Context): UsbManager =
        context.getSystemService(UsbManager::class.java)

    @Provides
    @Singleton
    fun provideCameraRepository(usbManager: UsbManager): CameraRepository =
        UsbCameraRepository(UsbCameraScanner(usbManager))

    @Provides
    @Singleton
    fun provideUsbPtpConnection(usbManager: UsbManager): UsbPtpConnection =
        UsbPtpConnection(usbManager)

    @Provides
    @Singleton
    fun provideCameraHeartbeat(usbManager: UsbManager, connectionFactory: UsbPtpConnection): CameraHeartbeat =
        CameraHeartbeat(usbManager, connectionFactory)

    @Provides
    @Singleton
    fun provideLocalStore(@ApplicationContext context: Context): LocalStore =
        LocalStore(context)

    @Provides
    @Singleton
    fun provideGitHubReleaseUpdater(@ApplicationContext context: Context): GitHubReleaseUpdater =
        GitHubReleaseUpdater(context, BuildConfig.GITHUB_REPO)

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
}
