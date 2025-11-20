package org.example.project.feature.work

import androidx.lifecycle.ViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf

class WorkDetailViewModel(private val workId: String) : ViewModel(), KoinComponent {

    private val repository: WorkDetailRepositoryImpl
        get() = get<WorkDetailRepositoryImpl> { parametersOf(workId) }

}