package com.salattracker.feature.developers

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.AppUiFontFamily
import com.salattracker.core.theme.SalatOnPrimary
import com.salattracker.core.theme.SalatOnSurface
import com.salattracker.core.theme.SalatOnSurfaceVariant
import com.salattracker.core.theme.SalatOutlineVariant
import com.salattracker.core.theme.SalatPrimary
import com.salattracker.core.theme.SalatSecondaryContainer
import com.salattracker.core.ui.SettingsCard

data class Developer(
    val name: String,
    val role: String,
    val bio: String,
    val location: String,
    val skills: List<String>,
    val photoRes: Int,
    val linkedInUrl: String,
)

val DEVELOPERS = listOf(
    Developer(
        name = "Mujahid Husnain",
        role = "Full Stack .NET Core Developer",
        bio = "Full Stack .NET Core Developer at Contour Software, working across Agentic AI, Azure AI Foundry, React, Angular and Android. Also shares developer content on YouTube.",
        location = "Mandi Bahauddin, Punjab, Pakistan",
        skills = listOf(".NET Core", "Agentic AI", "React", "Angular", "Android", "Firebase"),
        photoRes = R.drawable.dev_mujahid,
        linkedInUrl = "https://www.linkedin.com/in/mujahidhusnainkhattana/",
    ),
    Developer(
        name = "Saleha Malik",
        role = "Full-Stack Web Developer",
        bio = "IT graduate and Full-Stack Web Developer building with PHP, Laravel, Python and MySQL.",
        location = "Pakistan",
        skills = listOf("PHP", "Laravel", "Python", "MySQL"),
        photoRes = R.drawable.dev_saleha,
        linkedInUrl = "https://www.linkedin.com/in/saleha-malik-1a0961264/",
    ),
)

@Composable
fun DevelopersScreen(
    onLinkedInClick: (String) -> Unit,
    onContributeClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(
                painterResource(R.drawable.ic_code),
                contentDescription = null,
                tint = SalatPrimary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.developers_subtitle),
                color = SalatOnSurfaceVariant,
                fontSize = 13.sp,
                fontFamily = AppUiFontFamily,
            )
        }
        DEVELOPERS.forEach { developer ->
            DeveloperCard(developer = developer, onLinkedInClick = { onLinkedInClick(developer.linkedInUrl) })
        }
        SettingsCard {
            Text(
                stringResource(R.string.developer_contribute_title),
                color = SalatPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = AppUiFontFamily,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.developer_contribute_body),
                color = SalatOnSurfaceVariant,
                fontSize = 13.sp,
                fontFamily = AppUiFontFamily,
                lineHeight = 19.sp,
            )
            Spacer(Modifier.height(12.dp))
            com.salattracker.core.ui.SalatPrimaryButton(
                onClick = onContributeClick,
            ) {
                Text(stringResource(R.string.developer_contribute_button), fontFamily = AppUiFontFamily, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun DeveloperCard(developer: Developer, onLinkedInClick: () -> Unit) {
    SettingsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(developer.photoRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(48.dp).clip(CircleShape),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    developer.name,
                    color = SalatOnSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = AppUiFontFamily,
                )
                Text(
                    developer.role,
                    color = SalatOnSurfaceVariant,
                    fontSize = 12.sp,
                    fontFamily = AppUiFontFamily,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            developer.bio,
            color = SalatOnSurface,
            fontSize = 13.sp,
            fontFamily = AppUiFontFamily,
            lineHeight = 19.sp,
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(painterResource(R.drawable.ic_map_pin), contentDescription = null, tint = SalatOnSurfaceVariant, modifier = Modifier.size(16.dp))
            Text(
                developer.location,
                color = SalatOnSurfaceVariant,
                fontSize = 12.sp,
                fontFamily = AppUiFontFamily,
            )
        }
        if (developer.skills.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                developer.skills.forEach { skill -> SkillChip(skill) }
            }
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = SalatOutlineVariant)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onLinkedInClick)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(painterResource(R.drawable.ic_linkedin), contentDescription = null, tint = SalatPrimary, modifier = Modifier.size(20.dp))
            Text(
                stringResource(R.string.developer_view_linkedin),
                color = SalatPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = AppUiFontFamily,
            )
        }
    }
}

@Composable
private fun SkillChip(label: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = SalatSecondaryContainer,
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            color = SalatOnSurfaceVariant,
             = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = AppUiFontFamily,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DevelopersScreenPreview() {
    DevelopersScreen(onLinkedInClick = {}, onContributeClick = {})
}
