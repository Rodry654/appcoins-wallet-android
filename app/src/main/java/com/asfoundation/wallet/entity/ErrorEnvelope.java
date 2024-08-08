package com.asfoundation.wallet.entity;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import com.appcoins.wallet.core.utils.jvm_common.C;

public class ErrorEnvelope {
  public final int code;
  @Nullable public final String message;

  public ErrorEnvelope(int code, @Nullable String message) {
    this.code = code;
    this.message = message;
  }
}
