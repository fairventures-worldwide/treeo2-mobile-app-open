package org.treeo.treeo.ui.measure

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.treeo.treeo.repositories.DBMainRepository
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val dbMainRepository: DBMainRepository
) : ViewModel() {


}