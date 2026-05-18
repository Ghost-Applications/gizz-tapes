package gizz.tapes.ui.years

import gizz.tapes.data.Year

fun interface YearClicked {
    operator fun invoke(year: Year)
}
