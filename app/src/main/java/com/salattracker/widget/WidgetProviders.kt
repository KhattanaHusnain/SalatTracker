package com.salattracker.widget

class SmallMonoAllWidgetProvider : SalatWidgetProvider() { override val variant = WidgetVariant.SMALL_MONO_ALL }
class SmallMonoFocusWidgetProvider : SalatWidgetProvider() { override val variant = WidgetVariant.SMALL_MONO_FOCUS }
class SmallThemeAllWidgetProvider : SalatWidgetProvider() { override val variant = WidgetVariant.SMALL_THEME_ALL }
class SmallThemeFocusWidgetProvider : SalatWidgetProvider() { override val variant = WidgetVariant.SMALL_THEME_FOCUS }
class MediumMonoAllWidgetProvider : SalatWidgetProvider() { override val variant = WidgetVariant.MEDIUM_MONO_ALL }
class MediumMonoFocusWidgetProvider : SalatWidgetProvider() { override val variant = WidgetVariant.MEDIUM_MONO_FOCUS }
class MediumThemeAllWidgetProvider : SalatWidgetProvider() { override val variant = WidgetVariant.MEDIUM_THEME_ALL }
class MediumThemeFocusWidgetProvider : SalatWidgetProvider() { override val variant = WidgetVariant.MEDIUM_THEME_FOCUS }
class LargeMonoAllWidgetProvider : SalatWidgetProvider() { override val variant = WidgetVariant.LARGE_MONO_ALL }
class LargeMonoFocusWidgetProvider : SalatWidgetProvider() { override val variant = WidgetVariant.LARGE_MONO_FOCUS }
class LargeThemeAllWidgetProvider : SalatWidgetProvider() { override val variant = WidgetVariant.LARGE_THEME_ALL }
class LargeThemeFocusWidgetProvider : SalatWidgetProvider() { override val variant = WidgetVariant.LARGE_THEME_FOCUS }
class LockMonoAllWidgetProvider : SalatWidgetProvider() { override val variant = WidgetVariant.LOCK_MONO_ALL }
class LockMonoFocusWidgetProvider : SalatWidgetProvider() { override val variant = WidgetVariant.LOCK_MONO_FOCUS }
class LockThemeAllWidgetProvider : SalatWidgetProvider() { override val variant = WidgetVariant.LOCK_THEME_ALL }
class LockThemeFocusWidgetProvider : SalatWidgetProvider() { override val variant = WidgetVariant.LOCK_THEME_FOCUS }

fun providerClass(variant: WidgetVariant): Class<out SalatWidgetProvider> = when (variant) {
    WidgetVariant.SMALL_MONO_ALL -> SmallMonoAllWidgetProvider::class.java
    WidgetVariant.SMALL_MONO_FOCUS -> SmallMonoFocusWidgetProvider::class.java
    WidgetVariant.SMALL_THEME_ALL -> SmallThemeAllWidgetProvider::class.java
    WidgetVariant.SMALL_THEME_FOCUS -> SmallThemeFocusWidgetProvider::class.java
    WidgetVariant.MEDIUM_MONO_ALL -> MediumMonoAllWidgetProvider::class.java
    WidgetVariant.MEDIUM_MONO_FOCUS -> MediumMonoFocusWidgetProvider::class.java
    WidgetVariant.MEDIUM_THEME_ALL -> MediumThemeAllWidgetProvider::class.java
    WidgetVariant.MEDIUM_THEME_FOCUS -> MediumThemeFocusWidgetProvider::class.java
    WidgetVariant.LARGE_MONO_ALL -> LargeMonoAllWidgetProvider::class.java
    WidgetVariant.LARGE_MONO_FOCUS -> LargeMonoFocusWidgetProvider::class.java
    WidgetVariant.LARGE_THEME_ALL -> LargeThemeAllWidgetProvider::class.java
    WidgetVariant.LARGE_THEME_FOCUS -> LargeThemeFocusWidgetProvider::class.java
    WidgetVariant.LOCK_MONO_ALL -> LockMonoAllWidgetProvider::class.java
    WidgetVariant.LOCK_MONO_FOCUS -> LockMonoFocusWidgetProvider::class.java
    WidgetVariant.LOCK_THEME_ALL -> LockThemeAllWidgetProvider::class.java
    WidgetVariant.LOCK_THEME_FOCUS -> LockThemeFocusWidgetProvider::class.java
}
