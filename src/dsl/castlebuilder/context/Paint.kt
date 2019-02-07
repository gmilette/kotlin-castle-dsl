package dsl.castlebuilder.context

class Go {
    fun runIt() {
        val paint = Paint()
        paint.moveTo(10, 10)
        paint.lineTo(10, 20)
        paint.lineTo(20,20)
        paint.lineTo(10,20)

        val paintNoContext = Paint()
        paint.start(10, 10)
        paint.lineFromTo(10, 10, 10, 20)
        paint.lineFromTo(10,20,20,20)
        paint.lineFromTo(20, 20, 10,20)
    }
}

class Paint {
    fun start(startX: Number, startY: Number) {

    }
    fun moveFromTo(startX: Number, startY: Number, endX: Number, endY: Number) {

    }

    fun lineFromTo(startX: Number, startY: Number, endX: Number, endY: Number) {

    }


    fun moveTo(x: Number, y: Number) {

    }

    fun lineTo(x: Number, y: Number) {

    }
}