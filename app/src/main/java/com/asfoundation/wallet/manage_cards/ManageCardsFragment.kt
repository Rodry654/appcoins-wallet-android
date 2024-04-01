package com.asfoundation.wallet.manage_cards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Center
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.font.FontWeight.Companion.Medium
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.viewModels
import com.appcoins.wallet.ui.common.theme.WalletColors
import com.appcoins.wallet.ui.common.theme.WalletColors.styleguide_light_grey
import com.appcoins.wallet.ui.widgets.TopBar
import com.asf.wallet.R
import com.asfoundation.wallet.manage_cards.models.StoredCard
import com.wallet.appcoins.core.legacy_base.BasePageViewFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ManageCardsFragment : BasePageViewFragment() {

  private val viewModel: ManageCardsViewModel by viewModels()

  @Inject
  lateinit var manageCardsNavigator: ManageCardsNavigator

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    return ComposeView(requireContext()).apply {
      setContent { ManageCardsView() }
    }
  }

  @Composable
  fun ManageCardsView() {
    viewModel.getCards()
    Scaffold(
      topBar = {
        Surface { TopBar(isMainBar = false, onClickSupport = { viewModel.displayChat() }) }
      },
      containerColor = WalletColors.styleguide_blue,
    ) { padding ->
      when (val uiState = viewModel.uiState.collectAsState().value) {
        is ManageCardsViewModel.UiState.StoredCardsInfo ->
          ManageCardsContent(
            padding = padding, cardsList = uiState.storedCards
          )

        ManageCardsViewModel.UiState.Loading ->
          Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = CenterVertically,
            horizontalArrangement = Center
          ) {
            CircularProgressIndicator()
          }

        else -> {}
      }
    }
  }

  @Composable
  internal fun ManageCardsContent(padding: PaddingValues, cardsList: List<StoredCard>) {
    Column(
      modifier = Modifier
        .padding(padding)
        .padding(horizontal = 16.dp)
    ) {
      ScreenTitle()
      NewCardButton()
      ScreenSubtitle()
      StoredCardsList(cardsList)
    }
  }

  @Composable
  fun ScreenTitle() {
    Text(
      text = stringResource(R.string.manage_cards_header),
      modifier = Modifier.padding(8.dp),
      style = typography.headlineSmall,
      fontWeight = Bold,
      color = styleguide_light_grey,
    )
  }

  @Composable
  fun NewCardButton() {
    Card(
      colors = CardDefaults.cardColors(containerColor = WalletColors.styleguide_blue_secondary),
      onClick = {manageCardsNavigator.navigateToAddCard(/*navController TODO*/)},
      modifier = Modifier
        .padding(top = 24.dp)
        .fillMaxWidth()
        .height(56.dp)
    ) {
      Row {
        Image(
          modifier = Modifier
            .padding(16.dp)
            .align(CenterVertically),
          painter = painterResource(R.drawable.ic_add_card),
          contentDescription = stringResource(R.string.title_support),
        )
        Text(
          text = stringResource(R.string.manage_cards_add_title),
          modifier = Modifier
            .padding(8.dp)
            .align(CenterVertically),
          style = typography.bodyMedium,
          fontWeight = Medium,
          color = styleguide_light_grey,
        )
      }
    }
  }

  @Composable
  fun ScreenSubtitle() {
    Text(
      text = stringResource(R.string.manage_cards_view_manage_subtitle),
      modifier = Modifier
        .padding(top = 24.dp, start = 8.dp),
      style = typography.bodyMedium,
      fontWeight = Medium,
      fontSize = 12.sp,
      color = styleguide_light_grey,
    )
  }

  @Composable
  fun StoredCardsList(storedCards: List<StoredCard>) {
    LazyColumn {
      items(storedCards) { card ->
        PaymentCardItem(card)
      }
    }
  }

  @Composable
  fun PaymentCardItem(storedCard: StoredCard) {
    Card(
      colors = CardDefaults.cardColors(containerColor = WalletColors.styleguide_blue_secondary),
      modifier = Modifier
        .padding(top = 8.dp)
        .fillMaxWidth()
        .height(56.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Image(
          modifier = Modifier
            .padding(16.dp)
            .align(CenterVertically),
          painter = painterResource(storedCard.cardIcon),
          contentDescription = "Card icon",
        )
        Text(
          text = stringResource(R.string.manage_cards_card_ending_body, storedCard.cardLastNumbers),
          modifier = Modifier
            .padding(8.dp)
            .align(CenterVertically),
          style = typography.bodyMedium,
          fontWeight = Medium,
          color = styleguide_light_grey,
        )
        Spacer(modifier = Modifier.weight(1f))
        Image(
          modifier = Modifier
            .align(CenterVertically)
            .padding(end = 16.dp)
            .clickable { /* TODO */ }
          ,
          painter = painterResource(R.drawable.ic_delete_card),
          contentDescription = "Delete card",
        )
      }
    }
  }

  @Preview
  @Composable
  fun PreviewManageCardsContent() {
    ManageCardsContent(
      padding = PaddingValues(0.dp),
      cardsList = listOf(
        StoredCard("1234", R.drawable.ic_card_brand_visa),
        StoredCard("5678", R.drawable.ic_card_brand_master_card)
      )
    )
  }

}
