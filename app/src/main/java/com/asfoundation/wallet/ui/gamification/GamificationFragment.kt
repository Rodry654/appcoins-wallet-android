package com.asfoundation.wallet.ui.gamification

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.asf.wallet.R
import com.asfoundation.wallet.analytics.gamification.GamificationAnalytics
import dagger.android.support.DaggerFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class GamificationFragment : DaggerFragment(), GamificationView {

  @Inject
  lateinit var interactor: GamificationInteractor

  @Inject
  lateinit var analytics: GamificationAnalytics
  private lateinit var presenter: GamificationPresenter
  private lateinit var activityView: RewardsLevelView
  private lateinit var levelsAdapter: LevelsAdapter

  override fun onAttach(context: Context) {
    super.onAttach(context)
    require(
        context is RewardsLevelView) { GamificationFragment::class.java.simpleName + " needs to be attached to a " + RewardsLevelView::class.java.simpleName }
    activityView = context
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    presenter =
        GamificationPresenter(this, activityView, interactor, analytics, CompositeDisposable(),
            AndroidSchedulers.mainThread(), Schedulers.io())
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_gamification, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    presenter.present(savedInstanceState)
  }

  override fun displayGamificationInfo() {

  }

  override fun onDestroyView() {
    presenter.stop()
    super.onDestroyView()
  }
}
