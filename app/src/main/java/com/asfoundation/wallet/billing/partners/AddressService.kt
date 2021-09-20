package com.asfoundation.wallet.billing.partners

import io.reactivex.Single

interface AddressService {
  fun getStoreAddress(suggestedStoreAddress: String?): String

  fun getOemAddress(suggestedOemAddress: String?): String

  fun getAttributionEntity(packageName: String): Single<AttributionEntity>
}