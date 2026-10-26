package com.peptidetracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.peptidetracker.data.local.ActiveVialWithStockName
import com.peptidetracker.data.local.DoseLogWithStockName
import com.peptidetracker.data.local.PeptideStockEntity
import com.peptidetracker.domain.model.PeptideMath
import com.peptidetracker.domain.model.SyringeType
import com.peptidetracker.ui.viewmodels.PeptideViewModel
import java.text.DateFormat
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private enum class TrackerTab(val title: String) { INVENTORY("Inventory"), ACTIVE("Active vials"), CALCULATOR("Calculator") }

@Composable
fun PeptideTrackerScreen(viewModel: PeptideViewModel = viewModel()) {
    val stocks by viewModel.stocks.collectAsState()
    val activeVials by viewModel.activeVials.collectAsState()
    val recentDoses by viewModel.recentDoses.collectAsState()
    val message by viewModel.message.collectAsState()
    var tab by remember { mutableStateOf(TrackerTab.INVENTORY) }
    var reconstituteStock by remember { mutableStateOf<PeptideStockEntity?>(null) }
    var doseVial by remember { mutableStateOf<ActiveVialWithStockName?>(null) }
    var showAddStock by remember { mutableStateOf(false) }
    val suggestedInjectionSite by viewModel.suggestedInjectionSite.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { viewModel.refreshSuggestedInjectionSite() }
    LaunchedEffect(message) {
        message?.let {
            snackbarHost.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHost) }) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).padding(16.dp)) {
            Text("Peptide Tracker", style = MaterialTheme.typography.headlineMedium)
            Text("Offline inventory and personal logging", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TrackerTab.values().forEach { choice ->
                    if (tab == choice) Button(onClick = { tab = choice }) { Text(choice.title) }
                    else OutlinedButton(onClick = { tab = choice }) { Text(choice.title) }
                }
            }
            Spacer(Modifier.height(8.dp))
            when (tab) {
                TrackerTab.INVENTORY -> InventoryContent(
                    stocks = stocks,
                    onAdd = { showAddStock = true },
                    onReconstitute = { reconstituteStock = it },
                    onDelete = viewModel::deleteStock
                )
                TrackerTab.ACTIVE -> ActiveVialsContent(
                    vials = activeVials,
                    onDose = { doseVial = it },
                    onArchive = viewModel::archiveVial
                )
                TrackerTab.CALCULATOR -> CalculatorContent(viewModel, recentDoses)
            }
        }
    }

    if (showAddStock) AddStockDialog(onDismiss = { showAddStock = false }) { values ->
        viewModel.addStock(
            values.name, values.vendor, values.lot,
            values.vialMg, values.count, values.price, values.storage, values.expiration
        )
        showAddStock = false
    }
    reconstituteStock?.let { stock ->
        ReconstituteDialog(stock, onDismiss = { reconstituteStock = null }) { diluent, ml ->
            viewModel.reconstitute(stock.id, diluent, ml)
            reconstituteStock = null
        }
    }
    doseVial?.let { vial ->
        DoseLoggerDialog(vial, suggestedInjectionSite, onDismiss = { doseVial = null }) { mcg, syringe, site, notes ->
            viewModel.logDose(vial.id, mcg, syringe, site, notes)
            doseVial = null
        }
    }
}

@Composable
private fun InventoryContent(
    stocks: List<PeptideStockEntity>,
    onAdd: () -> Unit,
    onReconstitute: (PeptideStockEntity) -> Unit,
    onDelete: (Long) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("Add inventory") }
        if (stocks.isEmpty()) {
            EmptyMessage("No inventory yet. Add unopened stock to begin.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(stocks, key = { it.id }) { stock ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(stock.name, style = MaterialTheme.typography.titleLarge)
                            Text("${stock.unopenedCount} unopened · ${stock.vialMg} mg per vial")
                            Text("${stock.storageLocation} · ${stock.sourceVendor.ifBlank { "Vendor not specified" }}")
                            Text("Purchase: $${"%.2f".format(stock.purchasePriceUsd)} total")
                            stock.lotNumber?.let { Text("Lot: $it") }
                            stock.expirationDateEpoch?.let { Text("Expires ${DateFormat.getDateInstance().format(Date(it))}") }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onReconstitute(stock) }, enabled = stock.unopenedCount > 0) {
                                    Text("Reconstitute")
                                }
                                TextButton(onClick = { onDelete(stock.id) }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveVialsContent(
    vials: List<ActiveVialWithStockName>,
    onDose: (ActiveVialWithStockName) -> Unit,
    onArchive: (Long) -> Unit
) {
    if (vials.isEmpty()) {
        EmptyMessage("No active vials. Reconstitute an inventory item to see it here.")
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(vials, key = { it.id }) { vial ->
            val fraction = (vial.remainingVolumeMl / vial.bacVolumeMl).coerceIn(0.0, 1.0).toFloat()
            val daysLeft = PeptideMath.calculateDaysRemainingBeforeExpiry(vial.reconstitutionDateEpoch, vial.shelfLifeDays)
            val expired = daysLeft < 0
            val expiringSoon = daysLeft in 0..4
            val statusColor = when {
                expired -> Color(0xFFB3261E)
                expiringSoon -> Color(0xFF8A6500)
                else -> Color(0xFF237A57)
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(vial.stockName, style = MaterialTheme.typography.titleLarge)
                            Text("${vial.concentrationMgPerMl} mg/mL · ${vial.diluentType}")
                        }
                        Text(when {
                            expired -> "Expired"
                            expiringSoon -> "Expiring · ${daysLeft}d"
                            else -> "Fresh · ${daysLeft}d"
                        }, color = statusColor, style = MaterialTheme.typography.labelLarge)
                    }
                    LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                    Text("${"%.2f".format(vial.remainingVolumeMl)} mL remaining of ${"%.2f".format(vial.bacVolumeMl)} mL")
                    Text("Mixed ${DateFormat.getDateInstance().format(Date(vial.reconstitutionDateEpoch))}")
                    Row {
                        Button(onClick = { onDose(vial) }, enabled = !expired && vial.remainingVolumeMl > 0.0) {
                            Text("Log dose")
                        }
                        TextButton(onClick = { onArchive(vial.id) }) { Text("Archive") }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalculatorContent(
    viewModel: PeptideViewModel,
    recentDoses: List<DoseLogWithStockName>
) {
    val result by viewModel.scratchpad.collectAsState()
    var dose by remember { mutableStateOf("") }
    var concentration by remember { mutableStateOf("") }
    var syringe by remember { mutableStateOf(SyringeType.U100) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Syringe scratchpad", style = MaterialTheme.typography.titleLarge)
        Text("Calculation aid only. Verify measurements and instructions with a qualified professional.")
        OutlinedTextField(dose, { dose = it }, label = { Text("Amount (mcg)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(concentration, { concentration = it }, label = { Text("Concentration (mg/mL)") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SyringeType.entries.forEach { type ->
                if (type == syringe) Button(onClick = { syringe = type }) { Text(type.name) }
                else OutlinedButton(onClick = { syringe = type }) { Text(type.name) }
            }
        }
        Button(onClick = {
            viewModel.calculateScratchpad(dose.toDoubleOrNull() ?: Double.NaN,
                concentration.toDoubleOrNull() ?: Double.NaN, syringe)
        }) { Text("Calculate units") }
        result.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        result.units?.let { units ->
            Text("${"%.2f".format(units)} ${syringe.name.drop(1)} syringe units", style = MaterialTheme.typography.titleMedium)
            Text("${"%.3f".format(result.volumeMl)} mL")
            SyringeScale(units = units, maxUnits = syringe.unitsPerMl)
        }
        Spacer(Modifier.height(12.dp))
        Text("Recent dose log", style = MaterialTheme.typography.titleLarge)
        if (recentDoses.isEmpty()) Text("No doses logged yet.")
        recentDoses.forEach { dose ->
            val peptideName = dose.stockName ?: "Archived vial"
            Text("$peptideName · ${dose.doseMcg} mcg · ${DateFormat.getDateTimeInstance().format(Date(dose.timestampEpoch))} · ${dose.injectionSite}")
        }
    }
}

@Composable
private fun SyringeScale(units: Double, maxUnits: Double) {
    val clamped = (units / maxUnits).coerceIn(0.0, 1.0).toFloat()
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Canvas(Modifier.fillMaxWidth().height(54.dp)) {
            val centerY = size.height / 2f
            drawLine(androidx.compose.ui.graphics.Color.DarkGray, androidx.compose.ui.geometry.Offset(0f, centerY), androidx.compose.ui.geometry.Offset(size.width, centerY), 4.dp.toPx())
            for (tick in 0..20) {
                val x = size.width * tick / 20f
                val length = if (tick % 5 == 0) 24.dp.toPx() else 12.dp.toPx()
                drawLine(androidx.compose.ui.graphics.Color.DarkGray, androidx.compose.ui.geometry.Offset(x, centerY - length / 2), androidx.compose.ui.geometry.Offset(x, centerY + length / 2), 2.dp.toPx())
            }
            drawLine(androidx.compose.ui.graphics.Color(0xFF24745E), androidx.compose.ui.geometry.Offset(0f, centerY), androidx.compose.ui.geometry.Offset(size.width * clamped, centerY), 5.dp.toPx())
            drawCircle(androidx.compose.ui.graphics.Color(0xFF24745E), 7.dp.toPx(), androidx.compose.ui.geometry.Offset(size.width * clamped, centerY))
        }
        Text("0                                         ${maxUnits.roundToInt()} units")
    }
}

@Composable
private fun AddStockDialog(onDismiss: () -> Unit, onSave: (StockForm) -> Unit) {
    var name by remember { mutableStateOf("") }
    var vendor by remember { mutableStateOf("") }
    var lot by remember { mutableStateOf("") }
    var mg by remember { mutableStateOf("") }
    var count by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var storage by remember { mutableStateOf("") }
    var expiration by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add unopened stock") },
        text = {
            Column(Modifier.height(440.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FormField("Name", name) { name = it }
                FormField("Vendor", vendor) { vendor = it }
                FormField("Lot number (optional)", lot) { lot = it }
                FormField("Vial powder (mg)", mg) { mg = it }
                FormField("Unopened vial count", count) { count = it }
                FormField("Total price paid (USD)", price) { price = it }
                FormField("Storage location", storage) { storage = it }
                FormField("Expiration date (yyyy-MM-dd)", expiration) { expiration = it }
                if (expiration.isNotBlank() && parseExpirationDate(expiration) == null) {
                    Text("Enter a real date as yyyy-MM-dd", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsedMg = mg.toDoubleOrNull()
                val parsedCount = count.toIntOrNull()
                val parsedPrice = price.toDoubleOrNull()
                if (name.isNotBlank() && parsedMg != null && parsedCount != null && parsedPrice != null && storage.isNotBlank()) {
                    val expirationEpoch = parseExpirationDate(expiration)
                    if (expiration.isBlank() || expirationEpoch != null) {
                        onSave(StockForm(name, vendor, lot, parsedMg, parsedCount, parsedPrice, storage, expirationEpoch))
                    }
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ReconstituteDialog(stock: PeptideStockEntity, onDismiss: () -> Unit, onConfirm: (String, Double) -> Unit) {
    var diluent by remember { mutableStateOf("BAC Water") }
    var volume by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reconstitute ${stock.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("This consumes one unopened vial. Enter values from your product documentation.")
                FormField("Diluent type", diluent) { diluent = it }
                FormField("Diluent volume (mL)", volume) { volume = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { volume.toDoubleOrNull()?.let { if (diluent.isNotBlank() && it > 0) onConfirm(diluent, it) } }) { Text("Create active vial") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DoseLoggerDialog(vial: ActiveVialWithStockName, suggestedSite: String, onDismiss: () -> Unit, onSave: (Double, SyringeType, String, String) -> Unit) {
    var dose by remember { mutableStateOf("") }
    var site by remember(vial.id, suggestedSite) { mutableStateOf(suggestedSite) }
    var notes by remember { mutableStateOf("") }
    var syringe by remember { mutableStateOf(SyringeType.U100) }


    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log ${vial.stockName} dose") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${vial.concentrationMgPerMl} mg/mL · ${"%.2f".format(vial.remainingVolumeMl)} mL available")
                FormField("Dose amount (mcg)", dose) { dose = it }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SyringeType.values().forEach { type ->
                        if (type == syringe) Button(onClick = { syringe = type }) { Text(type.name) }
                        else OutlinedButton(onClick = { syringe = type }) { Text(type.name) }
                    }
                }
                FormField("Injection site", site) { site = it }
                TextButton(onClick = { site = suggestedSite }) { Text("Use suggested site: $suggestedSite") }
                FormField("Notes", notes) { notes = it }
                Text("Saving records this entry and subtracts its calculated volume from this vial.")
            }
        },
        confirmButton = {
            TextButton(onClick = { dose.toDoubleOrNull()?.let { if (it > 0 && site.isNotBlank()) onSave(it, syringe, site, notes) } }) { Text("Log dose") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun FormField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(value, onValueChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
}

@Composable
private fun EmptyMessage(message: String) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
    }
}

private data class StockForm(
    val name: String,
    val vendor: String,
    val lot: String,
    val vialMg: Double,
    val count: Int,
    val price: Double,
    val storage: String,
    val expiration: Long?
)

private fun parseExpirationDate(value: String): Long? {
    if (value.isBlank()) return null
    return runCatching {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        val position = ParsePosition(0)
        val parsed = parser.parse(value, position)
        require(parsed != null && position.index == value.length) { "Use a real date in yyyy-MM-dd format" }
        parsed.time
    }.getOrNull()
}

private fun suggestedSite(current: String): String {
    val sites = listOf("Abdomen - Right", "Abdomen - Left", "Thigh - Right", "Thigh - Left", "Deltoid - Right", "Deltoid - Left")
    return sites[(sites.indexOf(current).coerceAtLeast(-1) + 1) % sites.size]
}
