package com.asfoundation.wallet.verification.code

import android.os.Bundle
import io.reactivex.Observable

interface VerificationCodeView {

  fun updateUi(verificationCodeData: VerificationCodeData, savedInstance: Bundle?)

  fun showLoading()

  fun hideLoading()

  fun hideKeyboard()

  fun lockRotation()

  fun unlockRotation()

  fun showSuccess()

  fun showNetworkError()

  fun getMaybeLaterClicks(): Observable<Any>

  fun getChangeCardClicks(): Observable<Any>

  fun getConfirmClicks(): Observable<String>

  fun getSupportClicks(): Observable<Any>

  fun retryClick(): Observable<Any>

  fun setupUi(data: VerificationCodeData, savedInstanceState: Bundle?)

  fun showWrongCodeError()

  fun showVerificationCode()
}