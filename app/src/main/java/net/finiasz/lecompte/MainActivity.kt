package net.finiasz.lecompte

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.finiasz.lecompte.ui.theme.LeCompteEstBonTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LeCompteEstBonTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                    Plateau()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Plateau(gameViewModel : GameViewModel = viewModel()) {
    val state : GameState by gameViewModel.state.collectAsState()
    val reloadConfirmation = remember { mutableStateOf(false) }
    val solveConfirmation = remember { mutableStateOf(false) }

    val sizes : Sizes = with(LocalConfiguration.current) {
        Sizes(
            buttonWidthDp = this.screenWidthDp * .28f,
            buttonHeightDp = if (this.screenWidthDp > this.screenHeightDp) this.screenHeightDp * .072f else this.screenHeightDp * .068f,
            textSizeSp = this.screenHeightDp * .04f / this.fontScale
        )
    }

    Column(
        Modifier
            .padding(vertical = (sizes.buttonHeightDp/4).dp, horizontal = (sizes.buttonWidthDp/7).dp)
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier
                .fillMaxWidth()
        ) {
            for (i in 0..2) {
                Chiffre(
                    chiffres = state.chiffres,
                    usedChiffres = state.usedChiffres,
                    pos = i,
                    enabled = state.chiffreEnabled,
                    click = { gameViewModel.chiffreClick(i) },
                    sizes = sizes,
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier
                .fillMaxWidth()
        ) {
            for (i in 3..5) {
                Chiffre(
                    chiffres = state.chiffres,
                    usedChiffres = state.usedChiffres,
                    pos = i,
                    enabled = state.chiffreEnabled,
                    click = { gameViewModel.chiffreClick(i) },
                    sizes = sizes,
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier
                .fillMaxWidth()
                .height((sizes.buttonHeightDp*7).dp)
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .width((sizes.buttonWidthDp*(.6f/.28f)).dp)
                    .fillMaxHeight()
            ) {
                for (i in 0..4) {
                    Equation(
                        pos = i,
                        chiffres = state.chiffres,
                        usedChiffres = state.usedChiffres,
                        selected = state.selected,
                        selectedOps = state.selectedOps,
                        chiffreEnabled = state.chiffreEnabled,
                        won = state.won,
                        click = { gameViewModel.chiffreClick(6 + i) },
                        sizes = sizes,
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxHeight()
            ) {
                Op(op = "+", enabled = state.opEnabled, click = { gameViewModel.opClick(it) }, sizes = sizes)
                Op(op = "-", enabled = state.opEnabled, click = { gameViewModel.opClick(it) }, sizes = sizes)
                Op(op = "*", enabled = state.opEnabled, click = { gameViewModel.opClick(it) }, sizes = sizes)
                Op(op = "/", enabled = state.opEnabled, click = { gameViewModel.opClick(it) }, sizes = sizes)
            }
        }


        Row( horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .width((2 * sizes.buttonWidthDp).dp)
                    .height((1.8f * sizes.buttonHeightDp).dp)
                    .border(
                        if (state.won != null) 4.dp else 2.dp,
                        if (state.won != null) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.onSurface,
                        RoundedCornerShape(16.dp)
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(color = if (state.won != null) MaterialTheme.colorScheme.tertiary else Color.Transparent)
            ) {
                Text(
                    text = state.target.toString(),
                    modifier = Modifier
                        .align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onTertiary,
                    fontWeight = FontWeight.Bold,
                    fontSize = TextUnit(2 * sizes.textSizeSp, TextUnitType.Sp),
                )
            }
        }



        Row( horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Image(painter = painterResource(id = R.drawable.reset),
                contentDescription = null,
                modifier = Modifier
                    .width(sizes.buttonWidthDp.dp)
                    .height(sizes.buttonHeightDp.dp)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .background(color = MaterialTheme.colorScheme.error)
                    .clickable() {
                        if (state.won == null) {
                            reloadConfirmation.value = true
                        } else {
                            gameViewModel.reset()
                        }
                    }
                    .padding(vertical = (sizes.buttonHeightDp / 8).dp),
                colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.onError),
                contentScale = ContentScale.Fit
            )

            Image(painter = painterResource(id = R.drawable.solve),
                contentDescription = null,
                modifier = Modifier
                    .width(sizes.buttonWidthDp.dp)
                    .height(sizes.buttonHeightDp.dp)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .background(color = MaterialTheme.colorScheme.outlineVariant)
                    .clickable() {
                        solveConfirmation.value = true
                    }
                    .padding(vertical = (sizes.buttonHeightDp / 8).dp),
                colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.onError),
                contentScale = ContentScale.Fit
            )

            Image(painter = painterResource(id = R.drawable.undo),
                contentDescription = null,
                modifier = Modifier
                    .alpha(if (state.undoEnabled) 1f else .25f)
                    .width(sizes.buttonWidthDp.dp)
                    .height(sizes.buttonHeightDp.dp)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .background(color = MaterialTheme.colorScheme.tertiary)
                    .clickable(enabled = state.undoEnabled) { gameViewModel.undo() }
                    .padding(vertical = (sizes.buttonHeightDp / 8).dp),
                colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.onTertiary),
                contentScale = ContentScale.Fit
            )
        }
    }


    if (reloadConfirmation.value || solveConfirmation.value) {
        AlertDialog(
            onDismissRequest = {
                reloadConfirmation.value = false
                solveConfirmation.value = false
            }
        ) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = stringResource(id = if (solveConfirmation.value) R.string.solve_confirmation else R.string.reload_confirmation),
                    fontSize = TextUnit(24f, TextUnitType.Sp),
                    lineHeight = TextUnit(32f, TextUnitType.Sp),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Row(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            reloadConfirmation.value = false
                            solveConfirmation.value = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outline)
                    ) {
                        Text(
                            text = stringResource(id = R.string.cancel),
                            fontSize = TextUnit(24f, TextUnitType.Sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = {
                            if (reloadConfirmation.value) {
                                reloadConfirmation.value = false
                                gameViewModel.reset()
                            }
                            if (solveConfirmation.value) {
                                solveConfirmation.value = false
                                gameViewModel.solve()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (solveConfirmation.value) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.error)
                    ) {
                        Text(
                            text = stringResource(id = R.string.ok),
                            fontSize = TextUnit(24f, TextUnitType.Sp),
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalUnitApi::class)
@Composable
fun Chiffre(chiffres: List<Int?>, usedChiffres: List<Boolean>, won: Int? = null, pos: Int, enabled : Boolean = true, click : (Int) -> Unit, sizes: Sizes) {
    val value = chiffres[pos]
    val used = usedChiffres[pos]
    Box(modifier = Modifier
        .alpha(if (enabled) if (used) .5f else 1f else .25f)
        .width(sizes.buttonWidthDp.dp)
        .height(sizes.buttonHeightDp.dp)
        .let {
            if (won == pos) {
                it.border(
                    3.dp,
                    MaterialTheme.colorScheme.tertiaryContainer,
                    RoundedCornerShape(8.dp)
                )
            } else {
                it
            }
        }
        .clip(RoundedCornerShape(8.dp))
        .background(color = if (won == pos) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary)
        .clickable(enabled = enabled) { value?.let { click(it) } }
    ) {
        Text(
            text = value?.let { "$value" } ?: "",
            modifier = Modifier
                .align(Alignment.Center),
            color = if (won == pos) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimary,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            overflow = TextOverflow.Ellipsis,
            fontSize = TextUnit(sizes.textSizeSp, TextUnitType.Sp),
        )
        if (used) {
            Image(
                painter = painterResource(id = R.drawable.barred),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
        }
    }
}

@OptIn(ExperimentalUnitApi::class)
@Composable
fun Op(op: String, enabled : Boolean = true, click : (String) -> Unit, sizes: Sizes) {
    Image(
        painter = painterResource(id = when(op) {
            "+" -> R.drawable.add
            "-" -> R.drawable.subtract
            "*" -> R.drawable.multiply
            else -> R.drawable.divide
        }),
        contentDescription = null,
        colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.onSecondary),
        modifier = Modifier
            .alpha(if (enabled) 1f else .25f)
            .width(sizes.buttonWidthDp.dp)
            .height(sizes.buttonHeightDp.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color = MaterialTheme.colorScheme.secondary)
            .clickable(enabled = enabled) { click(op) }
            .padding(vertical = (sizes.buttonHeightDp / 8).dp),
        contentScale = ContentScale.FillHeight
    )
}

@Composable
fun Equation(pos: Int, chiffres: List<Int?>, usedChiffres: List<Boolean>, selected : List<Int?>, selectedOps : List<String?>, chiffreEnabled : Boolean, won: Int?, click: (Int) -> Unit, sizes : Sizes) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .border(1.dp, MaterialTheme.colorScheme.outline)
                .size((sizes.buttonWidthDp * .4f).dp, (sizes.buttonHeightDp / 1.7f).dp),
        ) {
            Text(
                text = selected[2 * pos]?.toString() ?: "",
                modifier = Modifier.align(Alignment.Center),
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                fontSize = TextUnit(sizes.textSizeSp / 2, TextUnitType.Sp)
            )
        }
        Image(
            painter = painterResource(id = when(selectedOps[pos]) {
                "+" -> R.drawable.add
                "-" -> R.drawable.subtract
                "*" -> R.drawable.multiply
                "/" -> R.drawable.divide
                else -> R.drawable.dot
            }),
            colorFilter = ColorFilter.tint(if (selectedOps[pos] == null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface),
            contentDescription = null,
            modifier = Modifier.size(width = (sizes.buttonWidthDp*.2f).dp, height = (sizes.buttonHeightDp*.5f).dp),
            contentScale = ContentScale.Fit,
        )
        Box(
            modifier = Modifier
                .border(1.dp, MaterialTheme.colorScheme.outline)
                .size((sizes.buttonWidthDp * .4f).dp, (sizes.buttonHeightDp / 1.7f).dp),
        ) {
            Text(
                text = selected[2 * pos + 1]?.toString() ?: "",
                modifier = Modifier.align(Alignment.Center),
                overflow = TextOverflow.Ellipsis,
                fontSize = TextUnit(sizes.textSizeSp / 2, TextUnitType.Sp)
            )
        }
        Text(
            text = "=",
            modifier = Modifier.width((sizes.buttonWidthDp/7).dp),
            textAlign = TextAlign.Center,
        )
        Chiffre(chiffres = chiffres, usedChiffres = usedChiffres, won = won, pos = pos + 6, enabled = chiffreEnabled && (selected.indexOf(null) == -1 || selected.indexOf(null)/ 2 > pos), click = click, sizes = sizes)
    }
}


@Composable
fun dpToSp(dp: Dp) = with(LocalDensity.current) { dp.toSp() }

data class Sizes(
    val buttonWidthDp: Float,
    val buttonHeightDp: Float,
    val textSizeSp: Float,
)