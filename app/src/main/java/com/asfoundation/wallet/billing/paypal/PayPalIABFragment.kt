package com.asfoundation.wallet.billing.paypal

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.fragment.app.viewModels
import com.airbnb.lottie.FontAssetDelegate
import com.airbnb.lottie.TextDelegate
import com.appcoins.wallet.billing.AppcoinsBillingBinder
import com.appcoins.wallet.billing.adyen.PaymentModel
import com.asf.wallet.R
import com.asf.wallet.databinding.FragmentPaypalBinding
import com.asfoundation.wallet.base.Async
import com.asfoundation.wallet.base.SingleStateFragment
import com.asfoundation.wallet.billing.adyen.PaymentType
import com.asfoundation.wallet.billing.adyen.PurchaseBundleModel
import com.asfoundation.wallet.entity.TransactionBuilder
import com.asfoundation.wallet.navigator.UriNavigator
import com.asfoundation.wallet.ui.iab.IabNavigator
import com.asfoundation.wallet.ui.iab.IabView
import com.asfoundation.wallet.ui.iab.Navigator
import com.asfoundation.wallet.ui.iab.WebViewActivity
import com.asfoundation.wallet.viewmodel.BasePageViewFragment
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Completable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.apache.commons.lang3.StringUtils
import java.math.BigDecimal
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class PayPalIABFragment(
//  val navigatorIAB: Navigator
)
  : BasePageViewFragment(),
  SingleStateFragment<PayPalIABState, PayPalIABSideEffect> {

  @Inject
  lateinit var navigator: PayPalIABNavigator

  @Inject
  lateinit var createPaypalTransactionUseCase: CreatePaypalTransactionUseCase  //TODO

  @Inject
  lateinit var createPaypalTokenUseCase: CreatePaypalTokenUseCase  //TODO

  @Inject
  lateinit var createPaypalAgreementUseCase: CreatePaypalAgreementUseCase  //TODO

  @Inject
  lateinit var waitForSuccessPaypalUseCase: WaitForSuccessPaypalUseCase  //TODO

  @Inject
  lateinit var createSuccessBundleUseCase: CreateSuccessBundleUseCase  //TODO

  @Inject
  lateinit var cancelPaypalTokenUseCase: CancelPaypalTokenUseCase  //TODO

  var networkScheduler = Schedulers.io()  //TODO

  var viewScheduler = AndroidSchedulers.mainThread() //TODO


  private val viewModel: PayPalIABViewModel by viewModels()

  private var binding: FragmentPaypalBinding? = null
  private val views get() = binding!!
  private lateinit var compositeDisposable: CompositeDisposable

  private lateinit var resultAuthLauncher: ActivityResultLauncher<Intent>

  private var authenticatedToken: String? = null

  private lateinit var iabView: IabView
  var navigatorIAB: Navigator? = null

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FragmentPaypalBinding.inflate(inflater, container, false)
    compositeDisposable = CompositeDisposable()
    registerWebViewResult()
    navigatorIAB = IabNavigator(parentFragmentManager, activity as UriNavigator?, iabView)
    return views.root
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)
    check(context is IabView) { "Paypal payment fragment must be attached to IAB activity" }
    iabView = context
  }

  private fun registerWebViewResult() {
    resultAuthLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.data?.dataString?.contains(PaypalReturnSchemas.RETURN.schema) == true) {
        Log.d(this.tag, "startWebViewAuthorization SUCCESS: ${result.data ?: ""}")
        startBillingAgreement()
      } else if (
        result.resultCode == Activity.RESULT_CANCELED ||
        (result.data?.dataString?.contains(PaypalReturnSchemas.CANCEL.schema) == true)
        ) {
        Log.d(this.tag, "startWebViewAuthorization CANCELED: ${result.data ?: ""}")
        authenticatedToken?.let {
          cancelPaypalTokenUseCase.invoke(it)
            .subscribe {
              iabView.close(null)
            }
        }
      }
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setListeners()
    handleBonusAnimation()
    showLoadingAnimation()
    startPayment()
  }


  override fun onStateChanged(state: PayPalIABState) {
    when (state.convertTotalAsync) {
      is Async.Uninitialized -> {

      }
      is Async.Loading -> {

      }
      is Async.Fail -> {

      }
      is Async.Success -> {
        state.convertTotalAsync.value?.let { convertedTotal ->

        }
      }
    }
  }

  private fun startPayment() {
    attemptTransaction(
      createTokenIfNeeded = true
    )
  }

  private fun createToken() {
    compositeDisposable.add(
      createPaypalTokenUseCase()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess {
          Log.d(this.tag, "Successful Token creation ")  //TODO event
          authenticatedToken = it.token
          startWebViewAuthorization(it.redirect.url)
        }
        .subscribe({}, {
          Log.d(this.tag, it.toString())    //TODO event
          showSpecificError(R.string.unknown_error)
        })
    )
  }

  private fun startBillingAgreement() {
    authenticatedToken?.let { authenticatedToken ->
      compositeDisposable.add(
        createPaypalAgreementUseCase(authenticatedToken)
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .doOnSuccess {
            Log.d(this.tag, "Successful Agreement creation: ${it.uid}")
            // after creating the billing agreement, don't create a new token if it fails
            attemptTransaction(createTokenIfNeeded = false)
          }
          .subscribe({}, {
            Log.d(this.tag, it.toString())    //TODO event
            showSpecificError(R.string.unknown_error)
          })
      )
    }
  }

  private fun attemptTransaction(createTokenIfNeeded: Boolean = true) {
    compositeDisposable.add(
      createPaypalTransactionUseCase(
        value = (amount.toString()),
        currency = currency,
        reference = transactionBuilder.orderReference,
        origin = origin,
        packageName = transactionBuilder.domain,
        metadata = transactionBuilder.payload,
        sku = transactionBuilder.skuId,
        callbackUrl = transactionBuilder.callbackUrl,
        transactionType = transactionBuilder.type,
        developerWallet = transactionBuilder.toAddress(),
        referrerUrl = transactionBuilder.referrerUrl
      )
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess {
          when(it?.validity) {
            PaypalTransaction.PaypalValidityState.COMPLETED -> {
              Log.d(this.tag, "Successful Paypal payment ") // TODO add event
              handleSuccess(it.hash, it.uid)
            }
            PaypalTransaction.PaypalValidityState.NO_BILLING_AGREEMENT -> {
              Log.d(this.tag, "No billing agreement. Create new token? $createTokenIfNeeded ")
              if (createTokenIfNeeded) {
                createToken()
              } else {
                Log.d(this.tag, "No paypal billing agreement")
                showSpecificError(R.string.unknown_error)
              }
            }
            PaypalTransaction.PaypalValidityState.PENDING -> {
              waitForSuccess(it.hash, it.uid)
            }
            PaypalTransaction.PaypalValidityState.ERROR -> {
              Log.d(this.tag, "Paypal transaction error")
              showSpecificError(R.string.unknown_error)
            }
            null -> {
              Log.d(this.tag, "Paypal transaction error")
              showSpecificError(R.string.unknown_error)
            }
          }
        }
        .subscribe({}, {
          Log.d(this.tag, it.toString())   //TODO event
          showSpecificError(R.string.unknown_error)
        })
    )
  }

  private fun setListeners() {
    views.paypalErrorButtons.errorBack.setOnClickListener {
      iabView.close(null)
    }
    views.paypalErrorButtons.errorCancel.setOnClickListener {
      iabView.close(null)
    }
  }

  private fun successBundle(
    hash: String?,
    orderReference: String?,
    purchaseUid: String?
  ): Single<PurchaseBundleModel> {
    return createSuccessBundleUseCase(
      transactionBuilder.type,
      transactionBuilder.domain,
      transactionBuilder.skuId,
      purchaseUid,
      orderReference,
      hash,
      networkScheduler
    )
  }

  private fun startWebViewAuthorization(url: String) {
    val intent = WebViewActivity.newIntent(requireActivity(), url)
    resultAuthLauncher.launch(intent)
  }

  private fun waitForSuccess(hash: String?, uid: String?) {
    compositeDisposable.add(waitForSuccessPaypalUseCase(uid ?: "")
//    .subscribeOn(networkScheduler)
//    .observeOn(viewScheduler)
      .subscribe(
        {
          when(it.status) {
            PaymentModel.Status.COMPLETED -> {
              Log.d(this.tag, "Settled transaction polling completed")
              handleSuccess(hash, uid)
            }
            PaymentModel.Status.FAILED, PaymentModel.Status.FRAUD, PaymentModel.Status.CANCELED,
            PaymentModel.Status.INVALID_TRANSACTION -> {
              Log.d(this.tag, "Error on transaction on Settled transaction polling")
            }
            else -> {}
          }

        },
        {
          Log.d(this.tag, "Error on Settled transaction polling")
        }))
  }

  private fun handleSuccess(hash: String? , uid: String?) {
    compositeDisposable.add(
      successBundle(hash, null, uid)
        .doOnSuccess {
//              sendPaymentEvent()
//              sendRevenueEvent()
        }
        .subscribeOn(networkScheduler)
        .observeOn(viewScheduler)
        .flatMapCompletable { bundle ->
          Completable.fromAction { showSuccessAnimation() }
            .andThen(Completable.timer(getAnimationDuration(), TimeUnit.MILLISECONDS))
            .andThen(Completable.fromAction {
              navigatorIAB?.popView(bundle.bundle)
            })
            .subscribeOn(AndroidSchedulers.mainThread())
        }
        .doOnError {
          // TODO event
          showSpecificError(R.string.unknown_error)
        }
        .subscribe()
    )

  }

  private fun showSuccessAnimation() {
    views.successContainer.iabActivityTransactionCompleted.visibility = View.VISIBLE
    views.loadingAuthorizationAnimation.visibility = View.GONE

  }

  private fun showLoadingAnimation() {
    views.successContainer.iabActivityTransactionCompleted.visibility = View.GONE
    views.loadingAuthorizationAnimation.visibility = View.VISIBLE

  }

  private fun showSpecificError(@StringRes stringRes: Int) {
    views.successContainer.iabActivityTransactionCompleted.visibility = View.GONE
    views.loadingAuthorizationAnimation.visibility = View.GONE

    val message = getString(stringRes)
    views.paypalErrorLayout.errorMessage.text = message
    views.paypalErrorLayout.root.visibility = View.VISIBLE
    views.paypalErrorButtons.root.visibility = View.VISIBLE
  }

  private fun getAnimationDuration() = views.successContainer.lottieTransactionSuccess.duration

  private fun handleBonusAnimation() {
    if (StringUtils.isNotBlank(bonus)) {
      views.successContainer.lottieTransactionSuccess.setAnimation(R.raw.transaction_complete_bonus_animation)
      setupTransactionCompleteAnimation()
    } else {
      views.successContainer.lottieTransactionSuccess.setAnimation(R.raw.success_animation)
    }
  }

  private fun setupTransactionCompleteAnimation() {
    val textDelegate = TextDelegate(views.successContainer.lottieTransactionSuccess)
    textDelegate.setText("bonus_value", bonus)
    textDelegate.setText(
      "bonus_received",
      resources.getString(R.string.gamification_purchase_completed_bonus_received)
    )
    views.successContainer.lottieTransactionSuccess.setTextDelegate(textDelegate)
    views.successContainer.lottieTransactionSuccess.setFontAssetDelegate(object : FontAssetDelegate() {
      override fun fetchFont(fontFamily: String): Typeface {
        return Typeface.create("sans-serif-medium", Typeface.BOLD)
      }
    })
  }

  override fun onSideEffect(sideEffect: PayPalIABSideEffect) = Unit

  private val amount: BigDecimal by lazy {
    if (requireArguments().containsKey(AMOUNT_KEY)) {
      requireArguments().getSerializable(AMOUNT_KEY) as BigDecimal
    } else {
      throw IllegalArgumentException("amount data not found")
    }
  }

  private val currency: String by lazy {
    if (requireArguments().containsKey(CURRENCY_KEY)) {
      requireArguments().getString(CURRENCY_KEY, "")
    } else {
      throw IllegalArgumentException("currency data not found")
    }
  }

  private val origin: String? by lazy {
    if (requireArguments().containsKey(ORIGIN_KEY)) {
      requireArguments().getString(ORIGIN_KEY)
    } else {
      throw IllegalArgumentException("origin not found")
    }
  }

  private val transactionBuilder: TransactionBuilder by lazy {
    if (requireArguments().containsKey(TRANSACTION_DATA_KEY)) {
      requireArguments().getParcelable<TransactionBuilder>(TRANSACTION_DATA_KEY)!!
    } else {
      throw IllegalArgumentException("transaction data not found")
    }
  }

  private val bonus: String by lazy {
    if (requireArguments().containsKey(BONUS_KEY)) {
      requireArguments().getString(BONUS_KEY, "")
    } else {
      throw IllegalArgumentException("bonus data not found")
    }
  }


  companion object {

    private const val PAYMENT_TYPE_KEY = "payment_type"
    private const val ORIGIN_KEY = "origin"
    private const val TRANSACTION_DATA_KEY = "transaction_data"
    private const val AMOUNT_KEY = "amount"
    private const val CURRENCY_KEY = "currency"
    private const val BONUS_KEY = "bonus"
    private const val PRE_SELECTED_KEY = "pre_selected"
    private const val IS_SUBSCRIPTION = "is_subscription"
    private const val IS_SKILLS = "is_skills"
    private const val FREQUENCY = "frequency"
    private const val GAMIFICATION_LEVEL = "gamification_level"
    private const val SKU_DESCRIPTION = "sku_description"
    private const val NAVIGATOR = "navigator"

    @JvmStatic
    fun newInstance(
      paymentType: PaymentType,
      origin: String?,
      transactionBuilder: TransactionBuilder,
      amount: BigDecimal,
      currency: String?,
      bonus: String?,
      isPreSelected: Boolean,
      gamificationLevel: Int,
      skuDescription: String,
      isSubscription: Boolean,
      isSkills: Boolean,
      frequency: String?,
    ): PayPalIABFragment = PayPalIABFragment().apply {
      arguments = Bundle().apply {
        putString(PAYMENT_TYPE_KEY, paymentType.name)
        putString(ORIGIN_KEY, origin)
        putParcelable(TRANSACTION_DATA_KEY, transactionBuilder)
        putSerializable(AMOUNT_KEY, amount)
        putString(CURRENCY_KEY, currency)
        putString(BONUS_KEY, bonus)
        putBoolean(PRE_SELECTED_KEY, isPreSelected)
        putInt(GAMIFICATION_LEVEL, gamificationLevel)
        putString(SKU_DESCRIPTION, skuDescription)
        putBoolean(IS_SUBSCRIPTION, isSubscription)
        putBoolean(IS_SKILLS, isSkills)
        putString(FREQUENCY, frequency)
      }
    }

  }

}
