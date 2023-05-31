package com.asfoundation.wallet.home.ui.list

import android.graphics.Rect
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.EpoxyAsyncUtil
import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.EpoxyViewHolder
import com.airbnb.epoxy.Typed2EpoxyController
import com.asf.wallet.R
import com.appcoins.wallet.core.arch.data.Async
import com.asfoundation.wallet.entity.GlobalBalance
import com.asfoundation.wallet.home.ui.list.header.HeaderModelGroup
import com.asfoundation.wallet.home.ui.list.transactions.LoadingModel_
import com.asfoundation.wallet.ui.widget.entity.TransactionsModel
import com.appcoins.wallet.core.utils.android_common.CurrencyFormatUtils

class HomeController : Typed2EpoxyController<Async<TransactionsModel>, Async<GlobalBalance>>(
    EpoxyAsyncUtil.getAsyncBackgroundHandler(), EpoxyAsyncUtil.getAsyncBackgroundHandler()) {

  private val formatter = CurrencyFormatUtils()

  var homeClickListener: ((HomeListClick) -> Unit)? = null

  override fun buildModels(txModelAsync: Async<TransactionsModel>,
                           balanceAsync: Async<GlobalBalance>
  ) {
    add(HeaderModelGroup(txModelAsync, balanceAsync, formatter, homeClickListener))

    when (txModelAsync) {
      Async.Uninitialized,
      is Async.Loading -> {
        val txModel = txModelAsync()
        if (txModel == null) {
          add(LoadingModel_().id("transactions_loading"))
        }
      }
      is Async.Fail -> Unit
      is Async.Success -> Unit
    }
  }

  override fun onModelBound(holder: EpoxyViewHolder, boundModel: EpoxyModel<*>, position: Int,
                            previouslyBoundModel: EpoxyModel<*>?) {
    super.onModelBound(holder, boundModel, position, previouslyBoundModel)
    // This is important because we use LayoutTransition.CHANGING to animate notifications
    // appearing / disappearing. However, this causes issues when fast scrolling because of recycling.
    // Disabling HeaderModelGroup recycling fixes this and is OK because there is only one HeaderModelGroup,
    // so there's not any recycling for that item anyway.
    if (position == 0) {
      holder.setIsRecyclable(false)
    }
  }

  override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
    super.onAttachedToRecyclerView(recyclerView)
    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
    recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
      override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        // We only care about the first position (header)
        val visiblePosition = layoutManager.findFirstVisibleItemPosition()
        if (visiblePosition == 0) {
          // Get the RV dimensions/location globally
          val rvRect = Rect()
          recyclerView.getGlobalVisibleRect(rvRect)
          // Get the WalletInfo dimension/location globally
          val itemRect = Rect()
          val view = layoutManager.findViewByPosition(0)
              ?.findViewById<ConstraintLayout>(R.id.wallet_info_root_layout)
          if (view != null && view.height > 0 && view.getGlobalVisibleRect(itemRect)) {
            // Retrieve the visibility extent (0f to 1f)
            val visibilityExtent = if (itemRect.bottom >= rvRect.bottom) {
              val visibleHeight = (rvRect.bottom - itemRect.top).toFloat()
              (visibleHeight / view.height).coerceAtMost(1f)
            } else {
              val visibleHeight = (itemRect.bottom - rvRect.top).toFloat()
              (visibleHeight / view.height).coerceAtMost(1f)
            }
            // Finally report it. We report that it's hidden with an high visibilityExtent (0.62)
            // because the toolbar actually hides it, although it is being shown from the perspective
            // of the RV. These visibilityExtent values are just empirical values that seem to work
            // nicely.
            if (visibilityExtent <= 0.62) {
              homeClickListener?.invoke(HomeListClick.ChangedBalanceVisibility(false))
            } else if (visibilityExtent > 0.78) {
              homeClickListener?.invoke(HomeListClick.ChangedBalanceVisibility(true))
            }
          }
        } else {
          homeClickListener?.invoke(HomeListClick.ChangedBalanceVisibility(false))
        }
      }
    })
  }

}