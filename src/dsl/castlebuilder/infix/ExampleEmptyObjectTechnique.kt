package dsl.castlebuilder.infix

fun main(args : Array<String>) {
    "hello".should(be).equal("hello")
    "hello" should be equal "hello"
}

object be

infix fun String.should(be: be): BeClause {
    return BeClause(this)
}

class BeClause(val compare: String) {
    infix fun equal(expected: String) {
        if (compare != expected) {
            throw RuntimeException("test fail")
        }
    }
}
