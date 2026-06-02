package com.paeki.fujirecipes.di

import android.content.Context
import android.hardware.usb.UsbManager
import com.paeki.fujirecipes.data.local.LocalStore
import com.paeki.fujirecipes.data.usb.CameraHeartbeat
import com.paeki.fujirecipes.data.usb.UsbCameraRepository
import com.paeki.fujirecipes.data.usb.UsbCameraScanner
import com.paeki.fujirecipes.data.usb.UsbPtpConnection
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
    fun provideUsbCameraRepository(usbManager: UsbManager): UsbCameraRepository =
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
}
