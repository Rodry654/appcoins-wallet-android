package com.asfoundation.wallet.analytics

import com.asfoundation.wallet.promotions.model.PromotionsModel

interface AnalyticsSetup {

  fun setUserId(walletAddress: String)

  fun setGamificationLevel(level: Int)

  fun setWalletOrigin(origin: PromotionsModel.WalletOrigin)
}