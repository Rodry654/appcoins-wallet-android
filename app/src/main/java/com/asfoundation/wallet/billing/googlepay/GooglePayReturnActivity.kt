package com.asfoundation.wallet.billing.googlepay

import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.asfoundation.wallet.billing.googlepay.models.CustomTabsPayResult
import com.asfoundation.wallet.billing.googlepay.repository.GooglePayWebRepository
import com.wallet.appcoins.core.legacy_base.BaseActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class GooglePayReturnActivity : BaseActivity() {
  @Inject
  lateinit var googlePayWebRepository: GooglePayWebRepository

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val data = intent.data
    if (Intent.ACTION_VIEW == intent.action && data != null) {
      val redirectResult = data.getQueryParameter("redirectResult")
      when (redirectResult) {
        CustomTabsPayResult.SUCCESS.key -> {
          Log.d(TAG, "success")
          googlePayWebRepository.saveChromeResult(CustomTabsPayResult.SUCCESS.key)
        }

        CustomTabsPayResult.CANCEL.key -> {
          googlePayWebRepository.saveChromeResult(CustomTabsPayResult.CANCEL.key)
        }

        CustomTabsPayResult.ERROR.key -> {
          googlePayWebRepository.saveChromeResult(CustomTabsPayResult.ERROR.key)
        }

        else -> {
          googlePayWebRepository.saveChromeResult(CustomTabsPayResult.ERROR.key)
        }
      }
    }
    finish()
  }

  companion object {
    val TAG = GooglePayReturnActivity::class.java.name
  }
}
