package dsl.castlebuilder.lambda

import dsl.castlebuilder.model.Connectable
import dsl.castlebuilder.model.StringSymbolTable

fun main(args : Array<String>) {
    CastleDsl().buildFourWallCastle()
}

class CastleDsl {

    fun buildDemo() {
        val castle = CastleBuilder()

        castle {
            moatDepth = 100

            keep {
                +"dungeon"

                hall {
                    capacity = 100
                    fireplace
                }
            }

            towers {
                tower("ne") {
                    catapult
                }
                drawBridge()
                tower("nw")
                tower("sw")
                tower("se")
            }
        }
    }

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

        val castleBuilt = castle.build()
        println("result: ${castleBuilt}")
    }
}

@DslMarker
annotation class CastleBuilderDsl

@CastleBuilderDsl
class CastleBuilder {
    var keep: KeepBuilder = KeepBuilder(this)
    var towers = TowerAccumulator(this)
    private var last: String? = null
    private var walls = mutableListOf<WallBuilder>()

    var moatDepth = 0

    private var buildings = mutableListOf<String>()

    operator fun invoke(initializer: CastleBuilder.() -> Unit) {
        initializer()
    }

    fun connect(from: String,
                to: String,
                drawBridge: DrawBridge? = null
    ): CastleBuilder {
        walls.add(WallBuilder(this, from, to, drawBridge))
        return this
    }

    fun to(to: String, drawBridge: DrawBridge? = null): CastleBuilder {
        last?.let {
            walls.add(WallBuilder(this, it, to, drawBridge))
        }
        last = to
        return this
    }


    fun String.to(to: String) {
        connect(this, to)
    }

    fun build() : Castle {
        val allTowers =  towers.towers.map { it.build() }

        // construct a symbol table so that the walls can reference created objects
        val symbols = StringSymbolTable<Connectable>()
        allTowers.forEach { symbols.add(it.name, it) }
        var keepBuild: Keep? = null
        keep?.let {
            keepBuild = it.build()
            symbols.add(it.name, keepBuild)
        }

        val allWalls = walls.map { it.build(symbols) }

        return Castle(keepBuild, allTowers, allWalls)
    }
}

open class CastleBuilderHolder(val castleBuilder: CastleBuilder) {
}

@CastleBuilderDsl
class TowerAccumulator(val castleBuilder: CastleBuilder) {
    var towers = mutableListOf<TowerBuilder>()
    var drawBridgeBetweenNext: DrawBridge? = null

    operator fun invoke(block: TowerAccumulator.() -> Unit) {
        block()
    }

    fun drawBridge() {
        drawBridgeBetweenNext = DrawBridge()
    }

    fun tower(name: String, initializer: TowerBuilder.() -> Unit = {}) {
        val tower = TowerBuilder(castleBuilder, name)
        tower.initializer()
        towers.add(tower)
        castleBuilder.to(name, drawBridgeBetweenNext)
        drawBridgeBetweenNext = null
    }
}

@CastleBuilderDsl
class TowerBuilder(castleBuilder: CastleBuilder,
                   override val name: String,
                   private var hasCatapult: Boolean = false
): CastleBuilderHolder(castleBuilder), Connectable {
    val catapult: Boolean
        get() {
            hasCatapult = true
            return true
        }

    fun build() : Tower {
        return Tower(name, hasCatapult)
    }
}

@CastleBuilderDsl
class WallBuilder(private val castleBuilder: CastleBuilder,
                  private val from: String,
                  var to: String? = null,
                  private var drawBridge: DrawBridge? = null) {

    fun build(symbols: StringSymbolTable<Connectable>) : Wall {
        to?.let {
            return Wall(symbols.lookup(from), symbols.lookup(it), drawBridge)
        }

        throw CastleWallNotConnectedException("wall ${from} needs and end")
    }
}

@CastleBuilderDsl
class KeepBuilder(private val castleBuilder: CastleBuilder,
                  val name: String = "keep") {
    var buildings = mutableListOf<KeepBuildingBuilder>()

    operator fun invoke(initializer: KeepBuilder.() -> Unit) {
        initializer()
    }

    operator fun String.unaryPlus() {
        buildings.add(NamedBuildingBuilder(this))
    }

    fun hall(initializer: HallBuilder.() -> Unit) {
        val hall = HallBuilder()
        hall.initializer()
        buildings.add(hall)
    }


    fun tower(to: String): KeepBuilder {
        castleBuilder.connect(name, to)
        return this
    }

    fun to(to: String) : KeepBuilder {
        castleBuilder.connect(name, to)
        return this
    }

    fun build() : Keep {
        val keepBuildings = buildings.map { it.build() }
        return Keep(name, keepBuildings)
    }
}

interface KeepBuildingBuilder {
    fun build(): KeepBuilding
}

class NamedBuildingBuilder(val name: String): KeepBuildingBuilder {
    override fun build(): KeepBuilding {
        return Named(name)
    }
}

@CastleBuilderDsl
class HallBuilder(val name: String = "",
                  var hasFireplace: Boolean = false,
                  var capacity: Int = 0) : KeepBuildingBuilder {
    var fireplace: Boolean = false
        get() {
            hasFireplace = true
            return true
        }
    override fun build(): KeepBuilding {
        return Hall(hasFireplace)
    }
}

data class Castle(var keep: Keep?, var towers: List<Tower>, var walls: List<Wall>) {
    override fun toString(): String {
        val sb = StringBuilder()
        keep?.let {
            sb.append("keep: ${it.name} with buildings\n")
            it.buildings.forEach {building ->
                sb.append(" ")
                sb.append(building.toString())
                sb.append("\n")
            }
        }
        sb.append("towers:\n")
        towers.forEach { tower ->
            sb.append(" ${tower.name} ${if (tower.hasCatapult) "with catapult" else ""}").append("\n")
        }
        sb.append("walls:\n")
        walls.forEach { wall ->
            sb.append(" ${wall.from.name} to ${wall.to.name} ${if (wall.drawBridge != null) "with drawbridge" else ""}").append("\n")
        }
        return sb.toString()
    }
}
data class Keep(override var name: String = "keep", var buildings: List<KeepBuilding>): Connectable
interface KeepBuilding
data class Hall(var fireplace: Boolean) : KeepBuilding
data class Named(var name: String) : KeepBuilding
data class Tower(override var name:String, var hasCatapult: Boolean): Connectable
data class Wall(var from: Connectable, var to: Connectable, var drawBridge: DrawBridge?)
class DrawBridge
class CastleWallNotConnectedException(message: String) : Exception(message)
