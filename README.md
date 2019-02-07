# kotlin-castle-dsl
Has some Kotlin Domain Specific Language code that demonstrates different ways to use Kotlin to create DSLs. Also it is described fully in this course: https://app.pluralsight.com/library/courses/kotlin-fundamentals-domain-specific-languages/

There are many sample DSLs in this repository that demonstrate different DSL techniques.

# Builder
Chain a bunch of function calls together to have dsl sentences.

```
      fun build() {
        var builder = CastleBuilder()

        // function sequence
        builder.keep("keep")
        builder.tower("sw")
        builder.tower("nw")
        builder.tower("ne")
        builder.tower("se")
        builder.connect("sw", "nw")
        builder.connect("nw", "ne")
        builder.connect("ne", "se")
        builder.connect("se", "sw")

        // apply
        builder.apply {
            keep("keep")
            tower("sw")
            tower("nw")
            tower("ne")
            tower("se")
            connect("sw", "nw")
            connect("nw", "ne")
            connect("ne", "se")
            connect("se", "sw")
            connect("keep", "nw")
            connect("keep", "nw")
            connect("keep", "se")
            connect("keep", "sw")
        }

        // builders
        builder = CastleBuilder()
        builder.keep("keep").tower("sw")
               .tower("nw").tower("ne").tower("se")
        builder.connect("keep", "sw").connect("keep", "ne")
               .connect("keep", "se").connect("keep", "sw")
        builder.connect("sw", "nw").connect("nw", "ne")
               .connect("ne", "se").connect("se", "sw")
        val castle = builder.build()
        println("result: $castle")

        // use varargs for this one
        builder.connectToAll("keep", "sw", "nw", "ne", "se")

        // using map syntax
        builder.connect(mapOf("sw" to "nw", "nw" to "ne",
                              "ne" to "se", "se" to "sw"))
    }  
```

# builders with Context
the dsl stores state in order to make function chains simpler
```
fun build() {
        val builder = CastleBuilder()

        // with context variable
        builder.start("sw").to("nw").to("ne").to("se")

        // using new context variable, for tracking which to start from
        builder.fix("keep")
               .fixTo("sw")
               .fixTo("nw")
               .fixTo("ne")
               .fixTo("se")

        // context function using partial application
        val to = builder.connectFrom("keep")
        to("sw")
        to("nw")
        to("ne")
        to("se")
    }
```

# Nested Builders
The dsl returns different builders to help create complex sentences

```
 fun build() {
        val builder = CastleBuilder()

        builder
            .tower("sw")
            .catapult()
            .wall()
            .tower("nw")
            .wall()
            .drawbridge()
            .tower("ne")
            .catapult()
            .wall()
            .tower("se")

        builder.tower("sw").catapult().wall().drawbridge().tower("ne")

        // used apply
        builder.keep().apply {
            tower("sw")
            tower("nw")
            tower("ne")
            tower("se")
        }

        val castle = builder.build()

        println("result: ${castle}")
    }
```

# Lambda and Setting Properties
The DSL uses lambdas to restrict the language and different ways of setting properties
```
    fun buildFourWallCastle() {

        val castle = CastleBuilder()
        castle {
            moatDepth = 100 // set a property normally

            keep {
                +"dungeon"

                hall {
                    fireplace // property
                    capacity = 10
                }
            }

            "keep".to("ne")
            "keep".to("nw")
            "keep".to("sw")
            "keep".to("se")

            towers {
                tower("ne") {
                    catapult
                }
                drawBridge()
                tower("nw")
                tower("sw") {
                    catapult
                }
                tower("se")
            }
            "se".to("ne")
        }
    }
```

# Infix notation
The DSL removes all syntactic noise and uses all infix notation. It also uses the blank object technique
to make compount clauses such as "has capacity" and "with drawbridge to"

```
fun build() {
        val castle = CastleBuilder()
        castle {

            keep has "dungeon"

            val hall = keep.hall()
            hall has capacity of 10
            hall has color of "black"
            hall has fireplace
            hall has kitchen

            "keep" to "ne"
            "keep" to "nw"
            "keep" to "sw"
            "keep" to "se"

            "ne" to "nw" with drawbridge to "sw" to "se" to "ne"
        }

        val castleBuilt = castle.build()
        println("result:\n${castleBuilt}")
    }
```
