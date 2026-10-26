package com.salattracker.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.core.ui.SettingsCard
import com.salattracker.core.ui.salatSwitchColors

/** Home feedback preferences shown in the app's drawer Settings screen. */
@Composable
internal fun HomeFeedbackSettingsCard(
    value: HomeAppearance,
    onChange: (HomeAppearance) -> Unit,
) {
    val effects = rememberHomeFeedbackEffects(value, configureVoice = true)
    Box(Modifier.fillMaxWidth().testTag("home-appearance-settings")) {
        SettingsCard {
            HomePreferenceSwitchRow(
                label = R.string.home_v27_show_message,
                detail = R.string.home_v27_show_message_detail,
                checked = value.showAppreciation,
                tag = "home-setting-message",
            ) { onChange(value.copy(showAppreciation = it)) }

            HomePreferenceSwitchRow(
                label = R.string.home_v8_voice,
                detail = R.string.home_v27_voice_detail,
                checked = value.voice,
                tag = "home-setting-voice",
            ) { onChange(value.copy(voice = it)) }

            Column(Modifier.alpha(if (value.voice) 1f else .45f)) {
                if (!effects.speechReady) {
                    Text(
                        stringResource(R.string.home_v8_voice_unavailable),
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = SalatOnSurfaceVariant,
                        fontFamily = AppUiFontFamily,
                        fontSize = 12.sp,
                    )
                } else {
                    var voiceMenu by remember { mutableStateOf(false) }
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.home_v8_voice),
                            Modifier.width(76.dp),
                            color = SalatOnSurfaceVariant,
                            fontFamily = AppUiFontFamily,
                            fontSize = 12.sp,
                        )
                        Box {
                            OutlinedButton(
                                onClick = { if (value.voice) voiceMenu = true },
                                modifier = Modifier.heightIn(min = 40.dp),
                            ) {
                                Text(
                                    value.voiceName ?: stringResource(R.string.home_v8_voice_auto),
                                    color = SalatOnSurface,
                                    fontFamily = AppUiFontFamily,
                                    fontSize = 12.sp,
                                )
                            }
                            DropdownMenu(voiceMenu, onDismissRequest = { voiceMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.home_v8_voice_auto), fontFamily = AppUiFontFamily) },
                                    onClick = { onChange(value.copy(voiceName = null)); voiceMenu = false },
                                )
                                effects.voices.forEach { (name, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label, fontFamily = AppUiFontFamily) },
                                        onClick = { onChange(value.copy(voiceName = name)); voiceMenu = false },
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.home_v8_speed),
                        Modifier.width(76.dp),
                        color = SalatOnSurfaceVariant,
                        fontFamily = AppUiFontFamily,
                        fontSize = 12.sp,
                    )
                    Slider(
                        value = value.speechRate,
                        onValueChange = { if (value.voice) onChange(value.copy(speechRate = it)) },
                        modifier = Modifier.weight(1f).testTag("home-setting-speech-rate"),
                        valueRange = .7f..1.2f,
                        steps = 9,
                    )
                }

                if (LocalConfiguration.current.locales[0].language in listOf("en", "fr")) {
                    HomePreferenceSwitchRow(
                        label = R.string.home_v27_arabic_words,
                        detail = R.string.home_v27_arabic_words_detail,
                        checked = value.arabicWords,
                        tag = "home-setting-arabic-words",
                    ) { onChange(value.copy(arabicWords = it)) }
                }

                val preview = stringResource(R.string.home_v8_well_done)
                Button(
                    onClick = { effects.play(preview, true) },
                    enabled = value.voice,
                    modifier = Modifier.heightIn(min = 40.dp).testTag("home-setting-preview"),
                ) {
                    Text(
                        stringResource(R.string.home_v8_preview),
                        fontFamily = AppUiFontFamily,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            HomePreferenceSwitchRow(
                label = R.string.home_v8_chime,
                detail = R.string.home_v27_chime_detail,
                checked = value.chime,
                tag = "home-setting-chime",
            ) { onChange(value.copy(chime = it)) }
            HomePreferenceSwitchRow(
                label = R.string.home_v8_confetti,
                detail = R.string.home_v27_confetti_detail,
                checked = value.confetti,
                tag = "home-setting-confetti",
            ) { onChange(value.copy(confetti = it)) }
            HomePreferenceSwitchRow(
                label = R.string.home_v8_vibration,
                detail = R.string.home_v27_vibration_detail,
                checked = value.vibration,
                tag = "home-setting-vibration",
                showDivider = false,
            ) { onChange(value.copy(vibration = it)) }
        }
    }
}

@Composable
private fun HomePreferenceSwitchRow(
    label: Int,
    detail: Int,
    checked: Boolean,
    tag: String,
    showDivider: Boolean = true,
    onChecked: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                stringResource(label),
                color = SalatOnSurface,
                fontFamily = AppUiFontFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(detail),
                color = SalatOnSurfaceVariant,
                fontFamily = AppUiFontFamily,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            modifier = Modifier.testTag(tag),
            colors = salatSwitchColors(),
        )
    }
    if (showDivider) HorizontalDivider(color = SalatOutlineVariant.copy(alpha = .4f))
}
