# CriptoFunHash v1

Herramienta didáctica para **calcular funciones hash y ver cómo funcionan por dentro**: MD5, SHA-1, SHA-2 y SHA-3.
Está pensada para la **Práctica 5** de *Seguridad en Tecnologías de la Información* (Universidad de Jaén) y sigue el
estilo de la herramienta CriptoRES: una ventana principal con ventanas internas, una para calcular y otra para seguir el algoritmo paso a paso.

Funciona con **Java 8 o superior** y no necesita instalar nada más.

## Qué puedes hacer con ella

| Ventana (menú / botón de la barra) | Para qué sirve |
|---|---|
| **Funciones hash** → MD5, SHA-1, SHA-2, SHA-3 | Calcular el resumen (hash) de un texto o de un fichero. Eliges la variante (por ejemplo SHA-256 o SHA3-512), escribes el texto o abres el fichero y pulsas **Transformar**. |
| **Seguimientos** → MD5, SHA-1, SHA-2, SHA-3 | Lo mismo, pero mostrando **todo el cálculo**: relleno, bloques, palabras del mensaje y cada ronda. Hay dos niveles: *a nivel de bloques* (resultado de cada bloque) y *a nivel de pasos* (todos los pasos del primer bloque). El texto se puede **guardar** o **imprimir**. En SHA-2 y SHA-3 hay además una pestaña **Vista gráfica** con colores, tablas de rondas y el estado 5×5 de Keccak. |
| **Análisis** → Resumen | El mismo mensaje con **todos los algoritmos a la vez**, y cómo cambia el resultado según la codificación del texto (UTF-8, Latin-1, UTF-16, ASCII). |
| **Análisis** → MD vs. esponja | Diagrama y tabla que comparan la construcción **Merkle–Damgård** (MD5, SHA-1, SHA-2) con la de **esponja** (SHA-3). |
| **Análisis** → Efecto avalancha | Invierte un único bit del mensaje (o edita el texto) y muestra **cuántos bits del resumen cambian**; lo normal es cerca del 50 %. |
| **Ataques** → Paradoja del cumpleaños | Simula la paradoja del cumpleaños (365 días, de 1 a 5000 iteraciones, con tabla de cumpleaños y gráfico de intentos y media) y además un **ataque de cumpleaños real**: busca dos mensajes distintos cuyos primeros *n* bits del resumen coinciden. |

Algoritmos incluidos: **MD5**, **SHA-1**, **SHA-224/256/384/512** y **SHA3-224/256/384/512**.
MD5 y SHA-1 están **obsoletos** (se conocen colisiones reales desde 2004 y 2017): se incluyen precisamente para comprobarlo.

## Cómo ejecutarla

```bash
java -jar criptofunhash.jar
```

Es una aplicación de escritorio con ventana propia: el `.jar` está incluido en esta carpeta y no hay que compilar nada.

### Compilarla (solo si quieres modificarla)

Necesitas un **JDK** (no solo el JRE) 8 o superior y **Maven**:

```bash
mvn -q test       # ejecuta los tests
mvn -q package    # genera target/criptofunhash.jar
```

## Guía rápida

1. Abre una ventana desde la barra de botones, por ejemplo **SHA-2** (cálculo) o **Seg. SHA-2** (seguimiento).
2. En **Datos de Entrada** elige *Cadena de Texto* (y su codificación) o *Fichero* (con **Examinar…**).
3. Pulsa **Transformar**. El resultado aparece en **Valor Hash o Resumen**.
4. En las ventanas de seguimiento, elige el *Tipo de Seguimiento* antes de transformar y cambia entre las pestañas
   *Datos sobre el proceso de Transformación*, *Seguimiento del Algoritmo* y *Vista gráfica*.
5. Con **Ventana → Cascada / Mosaico** puedes ordenar varias ventanas abiertas a la vez.

## Correspondencia con la Práctica 5

- **Actividad 3** (comprobar que MD5 y SHA-1 están rotos): abre el fichero `hello` y el fichero `erase` en la ventana
  **MD5**, y los dos PDF `shattered` en la ventana **SHA-1** (los resúmenes serán iguales). Después calcula el **SHA-256**
  de los cuatro ficheros (serán distintos).
- **Actividad 4** (SHA-256 y SHA-3): usa **Seg. SHA-2** y **Seg. SHA-3** con el texto «Hola mundo» y observa relleno, campo
  de longitud, palabras W₀…W₆₃, registros *a…h* y las rondas θ ρ π χ ι. El efecto avalancha está en **Análisis**.
- **Cuestionario, apartado 3**: la frase de ejemplo se analiza en el seguimiento de SHA-256; el cambio de un carácter, en
  **Efecto avalancha**; y la diferencia entre UTF-8 y Latin-1 (el carácter «ó»), en **Análisis → Resumen**.
- Los apartados sobre AES (AESphere) no forman parte de esta herramienta.

## Límites y cosas a tener en cuenta

- El seguimiento paso a paso y la vista gráfica funcionan con mensajes de **hasta 4096 bytes**; con ficheros más grandes
  solo se calcula el resumen. El tamaño máximo de fichero es de 512 MB.
- Con más de 16 bloques, el seguimiento a nivel de bloques detalla solo los 4 primeros y los 2 últimos.
- MD5 y SHA-1 tienen seguimiento solo en texto (sin vista gráfica), como en CriptoRES.
- Los resultados paso a paso se comprueban contra la implementación del JDK (`java.security.MessageDigest`). SHA-3 no existe
  en el JDK hasta Java 9, así que con Java 8 esa comprobación se omite con un aviso (el cálculo funciona igual).
- El valor hash mostrado es siempre el del mensaje completo.

## Estructura del proyecto

```
src/main/java/com/example/criptofunhash/
  model/        algoritmos (Md5, Sha1, Sha2, Sha3, Keccak), informes de seguimiento, avalancha, cumpleaños
  controller/   mensaje de entrada compartido
  ui/           ventana principal y ventanas internas
src/test/java/  tests JUnit (los algoritmos se contrastan con el JDK)
pom.xml         construcción con Maven (genera criptofunhash.jar)
```

## Autores y uso

Desarrollado por Francisco J. Martínez Mimbrera, Manuel J. Lucena López, Luis Gonzaga Pérez Cordón y
Cristóbal J. Carmona del Jesus (Universidad de Jaén).

**Software libre de uso educativo.** Puede utilizarse, copiarse y distribuirse sin permiso de los autores para fines
exclusivamente educativos y sin ánimo de lucro. Se ofrece «tal cual», sin garantía de ningún tipo. Esto es un aviso de uso,
no una licencia formal. El escudo de la Universidad de Jaén es propiedad de la Universidad de Jaén y no está incluido en
esta cesión de uso.
