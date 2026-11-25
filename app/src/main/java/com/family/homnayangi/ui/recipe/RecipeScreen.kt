package com.family.homnayangi.ui.recipe

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.family.homnayangi.domain.RecipeText
import com.family.homnayangi.ui.common.DishImage

/** F10 — xem cách nấu một món; Mẹ sửa hoặc thêm công thức được. */
@Composable
fun RecipeScreen(
    onBack: () -> Unit,
    viewModel: RecipeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf(false) }
    // Nút Back của máy khi đang sửa: thoát chế độ sửa, chưa rời màn hình.
    BackHandler(enabled = editing) { editing = false }

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
            IconButton(onClick = { if (editing) editing = false else onBack() }, modifier = Modifier.size(56.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", modifier = Modifier.size(30.dp))
            }
            Text(
                text = if (editing) "Sửa công thức" else "Cách nấu",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
        }

        val dish = state.dish
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            dish == null -> Text(
                "Không tìm thấy món này.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp),
            )
            editing -> RecipeEditor(state, onCancel = { editing = false }, onSave = { i, s, v ->
                if (viewModel.save(i, s, v)) editing = false
            })
            else -> RecipeView(state, onEdit = { editing = true })
        }
    }
}

@Composable
private fun RecipeView(state: RecipeUiState, onEdit: () -> Unit) {
    val dish = state.dish ?: return
    val recipe = state.recipe
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Ảnh món cỡ lớn, đúng tỉ lệ 4:3 của ảnh gốc.
        DishImage(
            dish = dish,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f),
        )
        Column {
            Text(dish.name, style = MaterialTheme.typography.headlineSmall)
            Text(
                "${dish.category.label} · ${RecipeText.cookTimeLabel(dish.cookTimeMin)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (recipe == null) {
            Text(
                "Món này chưa có công thức. Mẹ bấm \"Thêm công thức\" để ghi lại cách nấu của nhà mình.",
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            if (recipe.ingredients.isNotEmpty()) {
                SectionTitle("Nguyên liệu (cho ${state.servings} người)")
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    recipe.ingredients.forEach { line ->
                        Row {
                            Text("•  ", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)
                            Text(line, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            SectionTitle("Các bước nấu")
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                recipe.steps.forEachIndexed { index, step -> StepRow(index + 1, step) }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        state.videoUrl?.let { url ->
            Button(
                onClick = {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                    } catch (_: ActivityNotFoundException) {
                        // Máy không có trình duyệt / YouTube: bỏ qua.
                    }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (state.recipe?.videoUrl != null) "Xem video cách nấu" else "Tìm video trên YouTube",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Icon(Icons.Filled.Edit, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (recipe == null) "Thêm công thức" else "Sửa công thức", style = MaterialTheme.typography.labelLarge)
        }
        if (recipe?.isEdited == true) {
            Text(
                "Công thức này Mẹ đã sửa.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.size(8.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun StepRow(number: Int, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("$number", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary)
        }
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).padding(top = 2.dp))
    }
}

@Composable
private fun RecipeEditor(
    state: RecipeUiState,
    onCancel: () -> Unit,
    onSave: (ingredients: String, steps: String, video: String) -> Unit,
) {
    val recipe = state.recipe
    var ingredients by rememberSaveable { mutableStateOf(RecipeText.inputFromLines(recipe?.ingredients.orEmpty())) }
    var steps by rememberSaveable { mutableStateOf(RecipeText.inputFromLines(recipe?.steps.orEmpty())) }
    var video by rememberSaveable { mutableStateOf(recipe?.videoUrl.orEmpty()) }
    val stepsMissing = RecipeText.linesFromInput(steps).isEmpty()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(state.dish?.name.orEmpty(), style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = ingredients,
            onValueChange = { ingredients = it },
            label = { Text("Nguyên liệu — mỗi dòng một thứ") },
            textStyle = MaterialTheme.typography.bodyLarge,
            minLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = steps,
            onValueChange = { steps = it },
            label = { Text("Các bước nấu — mỗi dòng một bước") },
            textStyle = MaterialTheme.typography.bodyLarge,
            minLines = 6,
            isError = stepsMissing,
            supportingText = if (stepsMissing) {
                { Text("Cần ít nhất một bước nấu") }
            } else null,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = video,
            onValueChange = { video = it },
            label = { Text("Link video YouTube (không bắt buộc)") },
            textStyle = MaterialTheme.typography.bodyLarge,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.heightIn(min = 60.dp)) {
                Text("Huỷ", style = MaterialTheme.typography.labelLarge)
            }
            Button(
                onClick = { onSave(ingredients, steps, video) },
                enabled = !stepsMissing,
                modifier = Modifier.weight(1f).heightIn(min = 60.dp),
            ) {
                Text("Lưu công thức", style = MaterialTheme.typography.labelLarge)
            }
        }
        Spacer(Modifier.size(8.dp))
    }
}
