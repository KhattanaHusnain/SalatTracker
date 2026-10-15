package com.salattracker.core.locale

import com.salattracker.domain.model.AppLanguage
import com.salattracker.domain.repository.LocaleApplier
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocaleApplierImpl @Inject constructor() : LocaleApplier {
    override fun apply(language: AppLanguage) = LocaleManager.apply(language)
}
