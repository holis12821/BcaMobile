package id.bca.bcamobile.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.bca.bcamobile.data.account.AccountRepositoryImpl
import id.bca.bcamobile.core.security.AndroidLivenessAttestor
import id.bca.bcamobile.core.security.LivenessAttestor
import id.bca.bcamobile.core.security.PinKeySource
import id.bca.bcamobile.data.auth.AuthRepositoryImpl
import id.bca.bcamobile.data.auth.RemotePinKeySource
import id.bca.bcamobile.data.card.CardRepositoryImpl
import id.bca.bcamobile.data.content.ContentRepositoryImpl
import id.bca.bcamobile.data.config.AppConfigRepositoryImpl
import id.bca.bcamobile.data.ewallet.EWalletRepositoryImpl
import id.bca.bcamobile.data.notification.NotificationRepositoryImpl
import id.bca.bcamobile.data.onboarding.OnboardingRepositoryImpl
import id.bca.bcamobile.data.qris.QrisRepositoryImpl
import id.bca.bcamobile.data.transaction.TransactionRepositoryImpl
import id.bca.bcamobile.data.transfer.TransferRepositoryImpl
import id.bca.bcamobile.domain.account.AccountRepository
import id.bca.bcamobile.domain.auth.AuthRepository
import id.bca.bcamobile.domain.card.CardRepository
import id.bca.bcamobile.domain.config.AppConfigRepository
import id.bca.bcamobile.domain.content.ContentRepository
import id.bca.bcamobile.domain.ewallet.EWalletRepository
import id.bca.bcamobile.domain.notification.NotificationRepository
import id.bca.bcamobile.domain.onboarding.OnboardingRepository
import id.bca.bcamobile.domain.qris.QrisRepository
import id.bca.bcamobile.domain.transaction.TransactionRepository
import id.bca.bcamobile.domain.transfer.TransferRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindOnboardingRepository(
        impl: OnboardingRepositoryImpl,
    ): OnboardingRepository

    @Binds
    @Singleton
    abstract fun bindLivenessAttestor(impl: AndroidLivenessAttestor): LivenessAttestor

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    /**
     * Implementasinya di layer data, antarmukanya di `core/security` — supaya
     * `PinKeyProvider` tidak perlu mengenal Retrofit.
     */
    @Binds
    @Singleton
    abstract fun bindPinKeySource(impl: RemotePinKeySource): PinKeySource

    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds
    @Singleton
    abstract fun bindCardRepository(impl: CardRepositoryImpl): CardRepository

    @Binds
    @Singleton
    abstract fun bindContentRepository(impl: ContentRepositoryImpl): ContentRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        impl: TransactionRepositoryImpl,
    ): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindTransferRepository(impl: TransferRepositoryImpl): TransferRepository

    @Binds
    @Singleton
    abstract fun bindEWalletRepository(impl: EWalletRepositoryImpl): EWalletRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        impl: NotificationRepositoryImpl,
    ): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindQrisRepository(impl: QrisRepositoryImpl): QrisRepository

    @Binds
    @Singleton
    abstract fun bindAppConfigRepository(impl: AppConfigRepositoryImpl): AppConfigRepository
}
