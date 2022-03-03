package com.asfoundation.wallet.nfts.ui.nftTransactDialog

import androidx.fragment.app.Fragment
import com.asfoundation.wallet.nfts.domain.NFTItem
import com.asfoundation.wallet.nfts.usecases.EstimateNFTSendGasUseCase
import com.asfoundation.wallet.nfts.usecases.SendNFTUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent

@InstallIn(FragmentComponent::class)
@Module
class NFTTransactDialogModule {

  @Provides
  fun provideNFTTransactDialogViewModelFactory(data: NFTItem,
                                               estimateNFTSendGasUseCase: EstimateNFTSendGasUseCase,
                                               sendNFTUseCase: SendNFTUseCase): NFTTransactDialogViewModelFactory {
    return NFTTransactDialogViewModelFactory(data, estimateNFTSendGasUseCase, sendNFTUseCase)
  }

  @Provides
  fun provideNFTDetailsData(fragment: Fragment): NFTItem {
    fragment.requireArguments()
        .apply {
          return getSerializable(NFTTransactDialogFragment.NFT_ITEM_DATA)!! as NFTItem
        }
  }

}