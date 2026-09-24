package com.babasitaram.pro.autofill

import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveRequest

class BSRAutofillService : android.service.autofill.AutofillService() {
    override fun onFillRequest(request: FillRequest, cancellationSignal: android.os.CancellationSignal, callback: FillCallback) { callback.onSuccess(null) }
    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) { callback.onSuccess() }
}
