package com.appcoins.wallet.billing

import com.appcoins.wallet.bdsbilling.BdsApi
import com.appcoins.wallet.bdsbilling.ProxyService
import com.appcoins.wallet.bdsbilling.SubscriptionBillingService
import com.appcoins.wallet.bdsbilling.WalletService
import com.appcoins.wallet.bdsbilling.repository.BdsApiSecondary

interface BillingDependenciesProvider {
  fun getSupportedVersion(): Int

  fun getBdsApi(): BdsApi

  fun getWalletService(): WalletService

  fun getProxyService(): ProxyService

  fun getBillingMessagesMapper(): BillingMessagesMapper

  fun getBdsApiSecondary(): BdsApiSecondary

  fun getSubscriptionBillingService(): SubscriptionBillingService
}