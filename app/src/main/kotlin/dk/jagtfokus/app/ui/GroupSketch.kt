package dk.jagtfokus.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics

/** Pen sketch beside a group title. Decorative. Not status. Hidden from TalkBack. */
@Composable
fun GroupSketch(groupId: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    val tint = if (color == Color.Unspecified) androidx.compose.material3.MaterialTheme.colorScheme.onSurface else color
    Canvas(modifier.clearAndSetSemantics {}) {
        val strokes = SketchPen(size).also { ink(groupId, it, eye = false) }
        val eyes = SketchPen(size).also { ink(groupId, it, eye = true) }
        drawPath(
            strokes.path,
            tint,
            style = Stroke(
                width = maxOf(1.35f, size.minDimension * 0.058f),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
        drawPath(eyes.path, tint)
    }
}

private class SketchPen(private val size: Size) {
    val path = Path()
    private val inset = 0.04f

    private fun pt(x: Float, y: Float): Offset = Offset(
        size.width * (inset + x / 100f * (1 - inset * 2)),
        size.height * (inset + y / 100f * (1 - inset * 2)),
    )

    fun move(x: Float, y: Float) = path.moveTo(pt(x, y).x, pt(x, y).y)
    fun line(x: Float, y: Float) = path.lineTo(pt(x, y).x, pt(x, y).y)
    fun curve(c1x: Float, c1y: Float, c2x: Float, c2y: Float, x: Float, y: Float) {
        val end = pt(x, y)
        val c1 = pt(c1x, c1y)
        val c2 = pt(c2x, c2y)
        path.cubicTo(c1.x, c1.y, c2.x, c2.y, end.x, end.y)
    }

    fun dot(x: Float, y: Float, r: Float = 4.2f) {
        val c = pt(x, y)
        val rx = r * size.width * (1 - inset * 2) / 100f
        val ry = r * size.height * (1 - inset * 2) / 100f
        path.addOval(androidx.compose.ui.geometry.Rect(c.x - rx, c.y - ry, c.x + rx, c.y + ry))
    }
}

private fun ink(id: String, pen: SketchPen, eye: Boolean) {
    if (eye) {
        when (id) {
            "klovbaerende" -> pen.dot(68f, 60f)
            "rovdyr" -> pen.dot(58f, 52f)
            "ovrige_pattedyr" -> pen.dot(62f, 58f)
            "andefugle" -> pen.dot(78f, 42f)
            "gaes" -> pen.dot(66f, 18f)
            "honsefugle" -> pen.dot(84f, 50f)
            "ovrige_fugle" -> pen.dot(72f, 48f)
        }
        return
    }
    when (id) {
        "klovbaerende" -> stag(pen)
        "rovdyr" -> fox(pen)
        "ovrige_pattedyr" -> hare(pen)
        "andefugle" -> duck(pen)
        "gaes" -> goose(pen)
        "honsefugle" -> pheasant(pen)
        "ovrige_fugle" -> bird(pen)
        else -> pen.dot(50f, 50f, 18f)
    }
}

private fun stag(pen: SketchPen) {
    pen.move(44f, 48f); pen.curve(36f, 30f, 28f, 18f, 22f, 6f)
    pen.move(36f, 32f); pen.line(16f, 22f)
    pen.move(30f, 18f); pen.line(12f, 8f)
    pen.move(54f, 44f); pen.curve(64f, 26f, 70f, 14f, 78f, 4f)
    pen.move(62f, 28f); pen.line(80f, 18f)
    pen.move(70f, 14f); pen.line(86f, 6f)
    pen.move(40f, 50f); pen.curve(28f, 48f, 22f, 60f, 26f, 66f); pen.curve(32f, 64f, 38f, 58f, 42f, 54f)
    pen.move(42f, 54f); pen.curve(54f, 40f, 70f, 46f, 80f, 54f); pen.curve(90f, 58f, 94f, 64f, 92f, 70f)
    pen.curve(86f, 78f, 68f, 80f, 52f, 72f); pen.curve(42f, 66f, 36f, 60f, 42f, 54f)
    pen.move(50f, 74f); pen.curve(42f, 84f, 32f, 92f, 22f, 98f)
}

private fun fox(pen: SketchPen) {
    pen.move(20f, 48f); pen.line(12f, 14f); pen.line(32f, 42f); pen.line(40f, 40f); pen.line(48f, 10f); pen.line(62f, 38f)
    pen.curve(76f, 42f, 88f, 50f, 96f, 60f); pen.line(94f, 68f); pen.line(78f, 70f)
    pen.curve(58f, 76f, 34f, 72f, 22f, 60f); pen.curve(14f, 52f, 14f, 48f, 20f, 48f)
    pen.move(84f, 64f); pen.line(68f, 62f)
}

private fun hare(pen: SketchPen) {
    pen.move(32f, 50f); pen.curve(28f, 28f, 34f, 8f, 38f, 8f); pen.curve(44f, 18f, 44f, 36f, 44f, 50f)
    pen.move(50f, 50f); pen.curve(52f, 26f, 58f, 6f, 64f, 6f); pen.curve(70f, 18f, 64f, 36f, 62f, 50f)
    pen.move(30f, 54f); pen.curve(26f, 68f, 40f, 76f, 54f, 74f); pen.curve(70f, 72f, 80f, 62f, 76f, 52f)
    pen.curve(70f, 44f, 50f, 44f, 36f, 50f)
    pen.move(36f, 72f); pen.curve(20f, 80f, 22f, 96f, 42f, 98f); pen.curve(64f, 100f, 82f, 90f, 74f, 74f)
}

private fun duck(pen: SketchPen) {
    pen.move(20f, 60f); pen.curve(16f, 46f, 34f, 44f, 50f, 48f); pen.curve(64f, 44f, 72f, 52f, 70f, 58f)
    pen.curve(84f, 62f, 82f, 74f, 66f, 78f); pen.curve(44f, 84f, 18f, 76f, 20f, 62f)
    pen.move(20f, 56f); pen.line(8f, 48f); pen.line(12f, 60f)
    pen.move(64f, 52f); pen.curve(62f, 36f, 76f, 32f, 84f, 38f); pen.curve(92f, 44f, 90f, 56f, 74f, 56f)
    pen.move(82f, 42f); pen.line(98f, 46f); pen.line(82f, 50f)
    pen.move(32f, 60f); pen.curve(44f, 68f, 58f, 66f, 68f, 58f)
    pen.move(8f, 90f); pen.curve(24f, 82f, 40f, 90f, 56f, 98f); pen.curve(72f, 90f, 88f, 82f, 98f, 90f)
}

private fun goose(pen: SketchPen) {
    pen.move(16f, 70f); pen.curve(14f, 56f, 34f, 54f, 50f, 58f); pen.curve(66f, 54f, 76f, 66f, 72f, 76f)
    pen.curve(64f, 88f, 34f, 92f, 18f, 80f); pen.curve(10f, 74f, 12f, 72f, 16f, 70f)
    pen.move(48f, 62f); pen.curve(42f, 42f, 46f, 24f, 54f, 16f); pen.curve(60f, 10f, 72f, 10f, 74f, 20f)
    pen.line(92f, 24f); pen.line(74f, 28f); pen.curve(66f, 32f, 58f, 30f, 54f, 38f); pen.curve(50f, 48f, 52f, 56f, 54f, 62f)
    pen.move(18f, 64f); pen.line(6f, 54f)
}

private fun pheasant(pen: SketchPen) {
    pen.move(48f, 52f); pen.line(2f, 64f)
    pen.move(48f, 60f); pen.line(4f, 78f)
    pen.move(46f, 48f); pen.curve(62f, 42f, 74f, 48f, 76f, 58f); pen.curve(78f, 68f, 64f, 74f, 50f, 70f)
    pen.curve(38f, 66f, 38f, 56f, 46f, 48f)
    pen.move(72f, 54f); pen.curve(76f, 42f, 90f, 42f, 92f, 52f); pen.curve(94f, 60f, 84f, 64f, 74f, 56f)
    pen.move(84f, 44f); pen.line(88f, 32f)
    pen.move(92f, 50f); pen.line(100f, 52f)
}

private fun bird(pen: SketchPen) {
    pen.move(28f, 62f); pen.curve(24f, 48f, 42f, 44f, 56f, 50f); pen.curve(68f, 46f, 74f, 56f, 70f, 66f)
    pen.curve(64f, 78f, 42f, 82f, 28f, 72f); pen.curve(20f, 66f, 22f, 64f, 28f, 60f)
    pen.move(60f, 54f); pen.curve(62f, 40f, 74f, 38f, 80f, 46f); pen.curve(86f, 54f, 80f, 64f, 66f, 60f)
    pen.move(80f, 48f); pen.line(94f, 50f); pen.line(80f, 54f)
    pen.move(28f, 58f); pen.line(10f, 42f)
    pen.move(30f, 64f); pen.line(14f, 54f)
    pen.move(36f, 60f); pen.curve(48f, 68f, 60f, 66f, 68f, 58f)
}
