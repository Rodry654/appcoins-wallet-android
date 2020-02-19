package com.asfoundation.wallet;

import android.app.Activity;
import android.app.Service;
import android.os.Environment;
import androidx.fragment.app.Fragment;
import androidx.multidex.MultiDexApplication;
import com.appcoins.wallet.appcoins.rewards.AppcoinsRewards;
import com.appcoins.wallet.bdsbilling.ProxyService;
import com.appcoins.wallet.bdsbilling.WalletService;
import com.appcoins.wallet.bdsbilling.repository.BdsApiSecondary;
import com.appcoins.wallet.bdsbilling.repository.RemoteRepository;
import com.appcoins.wallet.billing.BillingDependenciesProvider;
import com.appcoins.wallet.billing.BillingMessagesMapper;
import com.asf.wallet.BuildConfig;
import com.asfoundation.wallet.di.DaggerAppComponent;
import com.asfoundation.wallet.poa.ProofOfAttentionService;
import com.asfoundation.wallet.ui.iab.AppcoinsOperationsDataSaver;
import com.asfoundation.wallet.ui.iab.InAppPurchaseInteractor;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.flurry.android.FlurryAgent;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasActivityInjector;
import dagger.android.HasServiceInjector;
import dagger.android.support.HasSupportFragmentInjector;
import io.fabric.sdk.android.Fabric;
import io.intercom.android.sdk.Intercom;
import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.plugins.RxJavaPlugins;
import java.io.File;
import java.io.IOException;
import javax.inject.Inject;
import org.jetbrains.annotations.NotNull;

public class App extends MultiDexApplication
    implements HasActivityInjector, HasServiceInjector, HasSupportFragmentInjector,
    BillingDependenciesProvider {

  @Inject DispatchingAndroidInjector<Activity> dispatchingActivityInjector;
  @Inject DispatchingAndroidInjector<Service> dispatchingServiceInjector;
  @Inject DispatchingAndroidInjector<Fragment> dispatchingFragmentInjector;
  @Inject ProofOfAttentionService proofOfAttentionService;
  @Inject InAppPurchaseInteractor inAppPurchaseInteractor;
  @Inject AppcoinsOperationsDataSaver appcoinsOperationsDataSaver;
  @Inject RemoteRepository.BdsApi bdsApi;
  @Inject WalletService walletService;
  @Inject ProxyService proxyService;
  @Inject AppcoinsRewards appcoinsRewards;
  @Inject BillingMessagesMapper billingMessagesMapper;
  @Inject BdsApiSecondary bdsapiSecondary;
  boolean logging = false;

  @Override public void onCreate() {
    super.onCreate();
    DaggerAppComponent.builder()
        .application(this)
        .build()
        .inject(this);
    setupRxJava();

    if (!BuildConfig.DEBUG) {
      new FlurryAgent.Builder().withLogEnabled(false)
          .build(this, BuildConfig.FLURRY_APK_KEY);
    }

    startLogToFile();

    Fabric.with(this, new Crashlytics.Builder().core(
        new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG)
            .build())
        .build());

    inAppPurchaseInteractor.start();
    proofOfAttentionService.start();
    appcoinsOperationsDataSaver.start();
    appcoinsRewards.start();

    Intercom.initialize(this, BuildConfig.INTERCOM_API_KEY, BuildConfig.INTERCOM_APP_ID);
    Intercom.client()
        .setInAppMessageVisibility(Intercom.Visibility.GONE);

  }

  private void setupRxJava() {
    RxJavaPlugins.setErrorHandler(throwable -> {
      if (throwable instanceof UndeliverableException) {
        if (BuildConfig.DEBUG) {
          throwable.printStackTrace();
        } else {
          FlurryAgent.onError("ID", throwable.getMessage(), throwable);
        }
      } else {
        throw new RuntimeException(throwable);
      }
    });
  }

  @Override public AndroidInjector<Activity> activityInjector() {
    return dispatchingActivityInjector;
  }

  @Override public AndroidInjector<Service> serviceInjector() {
    return dispatchingServiceInjector;
  }

  @Override public AndroidInjector<Fragment> supportFragmentInjector() {
    return dispatchingFragmentInjector;
  }

  @Override public int getSupportedVersion() {
    return BuildConfig.BILLING_SUPPORTED_VERSION;
  }

  @NotNull @Override public RemoteRepository.BdsApi getBdsApi() {
    return bdsApi;
  }

  @NotNull @Override public WalletService getWalletService() {
    return walletService;
  }

  @NotNull @Override public ProxyService getProxyService() {
    return proxyService;
  }

  @NotNull @Override public BillingMessagesMapper getBillingMessagesMapper() {
    return billingMessagesMapper;
  }

  @NotNull @Override public BdsApiSecondary getBdsApiSecondary() {
    return bdsapiSecondary;
  }

  public void startLogToFile() {
    if (!logging) {
      File logsDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
          .getAbsolutePath() + "/AppCoinsLogs");
      if (!logsDir.exists()) {
        logsDir.mkdirs();
      }
      try {
        String fileName = "walletLogs_" + System.currentTimeMillis() + ".txt";
        File logs = new File(logsDir, fileName);
        logs.createNewFile();
        String cmd = "logcat -f" + logs.getAbsolutePath() + " -v time";
        Runtime.getRuntime()
            .exec(cmd);
        logging = true;
      } catch (IOException e) {
        logging = false;
        e.printStackTrace();
      }
    }
  }
}
