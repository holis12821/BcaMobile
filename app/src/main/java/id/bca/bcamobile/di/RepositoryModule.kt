package id.bca.bcamobile.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.bca.bcamobile.data.account.AccountRepositoryImpl
import id.bca.bcamobile.data.auth.AuthRepositoryImpl
import id.bca.bcamobile.data.ewallet.EWalletRepositoryImpl
import id.bca.bcamobile.data.onboarding.OnboardingRepositoryImpl
import id.bca.bcamobile.data.transaction.TransactionRepositoryImpl
import id.bca.bcamobile.data.transfer.TransferRepositoryImpl
import id.bca.bcamobile.domain.account.AccountRepository
import id.bca.bcamobile.domain.auth.AuthRepository
import id.bca.bcamobile.domain.ewallet.EWalletRepository
import id.bca.bcamobile.domain.onboarding.OnboardingRepository
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
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

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
}
