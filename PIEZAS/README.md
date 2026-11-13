# PIEZAS — Librería de conjuntos de piezas de CHESS-X

Librería **autónoma y autocontenida** con los 12 conjuntos de piezas que usa CHESS-X
(oficiales de Lichess, convertidos a VectorDrawable de Android).

**Sin dependencias** de CHESS-X ni de ninguna app: solo modelos propios + `R.drawable`.

## Contenido

```
PIEZAS/
├── res/drawable/          144 VectorDrawable: ps_<set>_<w|b><pieza>.xml
├── PieceSets.kt           Código autocontenido (modelos + enum + mapeo)
└── README.md              Este archivo
```

Cada conjunto son **12 vectores** (6 piezas × 2 colores):
`p=peón · n=caballo · b=alfil · r=torre · q=dama · k=rey`
y `w=blanca · b=negra`.

Conjuntos incluidos:

| PieceSet  | resKey     | Nombre (ES / EN) |
|-----------|------------|------------------|
| LICHESS   | cburnett   | Clásico / Classic |
| ALPHA     | alpha      | Alpha / Alpha |
| ANARCANDY | anarcandy  | Anarcandy / Anarcandy |
| COMPANION | companion  | Cómplice / Companion |
| HORSEY    | horsey     | Horsey / Horsey |
| LEIPZIG   | leipzig    | Leipzig / Leipzig |
| MERIDA    | merida     | Merida / Merida |
| PIXEL     | pixel      | Pixel / Pixel |
| RIOHACHA  | riohacha   | Riohacha / Riohacha |
| STAUNTY   | staunty    | Staunty / Staunty |
| TATIANA   | tatiana    | Tatiana / Tatiana |
| ICPIECES  | icpieces   | Icpieces / Icpieces |

## Instalación en un proyecto Android

1. Copia `res/drawable/ps_*.xml` (los 144 archivos) a `app/src/main/res/drawable/` de tu proyecto.
2. Copia `PieceSets.kt` a tu módulo (cualquier paquete sirve; el ejemplo usa `com.piezas`).
3. Ajusta el paquete y descomenta la línea `import TU_PAQUETE.R` en `PieceSets.kt`.
4. Úsalo:

```kotlin
// Jetpack Compose
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.piezas.*

Image(
    painter = painterResource(pieceDrawableId(ChessPiece.KNIGHT, ChessColor.WHITE, PieceSet.LICHESS)),
    contentDescription = "Caballo blanco"
)

// Vista clásica
val img = findViewById<ImageView>(R.id.pieza)
img.setImageResource(pieceDrawableId(ChessPiece.KNIGHT, ChessColor.WHITE, PieceSet.LICHESS))

// Recorrido de todas las combinaciones
for (set in PieceSet.entries) {
    for (color in ChessColor.entries) {
        for (piece in ChessPiece.entries) {
            pieceDrawableId(piece, color, set)
        }
    }
}
```

## Notas importantes

### Resource shrinking
Si tu proyecto activa `shrinkResources`, usa **siempre** `pieceDrawableId()` (remapeo estático a
`R.drawable.*`): el linker de recursos ve las referencias y no borra los vectores. La variante
`pieceDrawableIdByName()` (lookup por nombre con cache) **no** es segura con shrinking, porque el
compilador no detecta que los drawables se usan por nombre.

### Nombres de archivo
Mantén los nombres `ps_<resKey>_<w|b><pieza>` exactamente: el código los usa tal cual.
Si renombras un conjunto, cambia su `resKey` en el enum `PieceSet`.

### Verificación rápida
Los 144 drawables están verificados: cada `R.drawable.ps_*` del código tiene su archivo y viceversa.

## Procedencia y licencia

Los vectores son los **conjuntos oficiales de la comunidad de Lichess**, convertidos a
VectorDrawable con el conversor del proyecto CHESS-X (`tools/svg2vd.py`). Cada conjunto conserva
la licencia de su autor original en Lichess (en su mayoría CC-BY-SA / CC0 / GPL según el conjunto);
ante un uso público, atribuye a Lichess y a los autores de cada conjunto.

## ¿Qué incluye el uso típico?
- `pieceDrawableId(piece, color, set): Int` → el drawable (cualquier API de Android)
- `PieceSet` → los 12 conjuntos con etiquetas ES/EN y `resKey`
- `ChessPiece` / `ChessColor` → tipos propios + `code()` (p/n/b/r/q/k y w/b)
- `pieceDrawableIdByName(...)` → variante dinámica (no compatible con shrinking)
