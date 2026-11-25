package com.family.homnayangi.ui.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.family.homnayangi.domain.RecipeText
import com.family.homnayangi.domain.TextNormalizer
import com.family.homnayangi.domain.model.DishCategory
import com.family.homnayangi.domain.model.DishInfo
import com.family.homnayangi.ui.common.DishImage

/** Bảng chọn món để Mẹ thêm vào mâm: tìm không dấu + lọc theo loại. Bấm một món là thêm luôn và đóng bảng. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDishSheet(
    dishes: List<DishInfo>,
    onPick: (DishInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<DishCategory?>(null) }

    val shown = remember(dishes, query, category) {
        val order = listOf(DishCategory.MAN, DishCategory.XAO, DishCategory.RAU, DishCategory.CANH)
        dishes.filter { (category == null || it.category == category) && TextNormalizer.matches(TextNormalizer.noAccent(it.name), query) }
            .sortedWith(compareBy<DishInfo> { order.indexOf(it.category) }.thenBy { TextNormalizer.noAccent(it.name) })
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Thêm món vào mâm",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Tìm món, vd: rau muong", style = MaterialTheme.typography.bodyLarge) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { query = "" }, modifier = Modifier.size(56.dp)) {
                            Icon(Icons.Filled.Clear, contentDescription = "Xoá chữ tìm")
                        }
                    }
                } else null,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                (listOf<DishCategory?>(null) + DishCategory.entries).forEach { c ->
                    FilterChip(
                        selected = category == c,
                        onClick = { category = c },
                        label = { Text(c?.label ?: "Tất cả", style = MaterialTheme.typography.labelMedium) },
                        leadingIcon = if (category == c) {
                            { Icon(Icons.Filled.Check, contentDescription = null) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }

            if (shown.isEmpty()) {
                Text(
                    if (query.isNotBlank()) "Không tìm thấy món nào khớp \"$query\"." else "Không còn món nào để thêm.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                items(shown, key = { it.id }) { dish ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 72.dp)
                            .clickable(onClickLabel = "Thêm vào mâm") { onPick(dish) }
                            .padding(vertical = 6.dp),
                    ) {
                        DishImage(dish, MaterialTheme.shapes.medium, Modifier.size(width = 88.dp, height = 66.dp))
                        Column(Modifier.weight(1f)) {
                            Text(dish.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                RecipeText.cookTimeLabel(dish.cookTimeMin),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}
