package dsl.castlebuilder.infix

import dsl.castlebuilder.model.Connectable
import dsl.castlebuilder.model.StringSymbolTable
import dsl.castlebuilder.infix.HallBuilder.Feature.*
import dsl.castlebuilder.model.DrawBridge

fun main(args : Array<String>) {
    CastleDsl().build()
}

class CastleDsl {
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
}

@DslMarker
annotation class CastleBuilderDsl

object drawbridge

@CastleBuilderDsl
class CastleBuilder {
    var keep: KeepBuilder = KeepBuilder(this)
    private var walls = mutableListOf<WallBuilder>()

    operator fun invoke(initializer: CastleBuilder.() -> Unit) {
        initializer()
    }

    fun connect(from: String, to: String, drawBridge: DrawBridge? = null): WallBuilder {
        val builder = WallBuilder(this, from, to, drawBridge)
        walls.add(builder)
        return builder
    }

    infix fun String.to(to: String) : WallBuilder {
        return connect(this, to)
    }

    fun build() : Castle {
        val towerNames = mutableSetOf<String>()
        walls.forEach {
            if (it.from != keep.name) {
                towerNames.add(it.from)
                it.to?.let { to ->
                    towerNames.add(to)
                }
            }
        }

        val allTowers =  towerNames.map { Tower(it) }

        // construct a symbol table so that the walls can reference created objects
        val symbols = StringSymbolTable<Connectable>()
        allTowers.forEach { symbols.add(it.name, it) }
        var keepBuild: Keep? = null
        keep.let {
            keepBuild = it.build()
            symbols.add(it.name, keepBuild)
        }

        val allWalls = walls.map { it.build(symbols) }

        return Castle(keepBuild, allTowers, allWalls)
    }
}

class WallBuilder(private val castleBuilder: CastleBuilder, val from: String, var to: String? = null, private var drawBridge: DrawBridge? = null) {
    infix fun with(drawBridge: drawbridge) : WallBuilder {
        this.drawBridge = DrawBridge()
        return this
    }

    infix fun to(next: String) : WallBuilder {
        to?.let {
            return castleBuilder.connect(it, next)
        }
        return this
    }

    fun build(symbols: StringSymbolTable<Connectable>) : Wall {
        to?.let {
            return Wall(symbols.lookup(from), symbols.lookup(it), drawBridge)
        }

        throw CastleWallNotConnectedException("wall ${from} needs and end")
    }
}

class KeepBuilder(private val castleBuilder: CastleBuilder, val name: String = "keep") {
    var buildings = mutableListOf<KeepBuildingBuilder>()

    operator fun invoke(initializer: KeepBuilder.() -> Unit) {
        initializer()
    }

    operator fun String.unaryPlus() {
        buildings.add(NamedBuildingBuilder(this))
    }

    infix fun has(name: String) {
        buildings.add(NamedBuildingBuilder(name))
    }

    fun hall() : HallBuilder {
        val hall = HallBuilder()
        buildings.add(hall)
        return hall
    }

    fun tower(to: String): KeepBuilder {
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

object capacity
object color

class HallBuilder(val name: String = "", var features: Set<Feature> = mutableSetOf(), var capacity: Int = 0, var color: String = "white") : KeepBuildingBuilder {
    enum class Feature {
        fireplace, diningroom, kitchen
    }

    infix fun has(feature: Feature) {
        when (feature) {
            fireplace -> features += fireplace
            diningroom -> features += diningroom
            kitchen -> features += kitchen
        }
    }

    infix fun has(prop: capacity) : HasCapacityClause {
        return HasCapacityClause(this)
    }

    infix fun has(prop: color) : HasColorClause {
        return HasColorClause(this)
    }

    class HasCapacityClause(val hallBuilder: HallBuilder) {
        infix fun of(capacity: Int) {
            hallBuilder.capacity = capacity
        }
    }

    class HasColorClause(val hallBuilder: HallBuilder) {
        infix fun of(color: String) {
            hallBuilder.color = color
        }
    }

    override fun build(): KeepBuilding {
        return Hall(features, color, capacity)
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
data class Hall(var features: Set<HallBuilder.Feature>, val color: String, val capacity: Int) : KeepBuilding
data class Named(var name: String) : KeepBuilding
data class Tower(override var name:String, var hasCatapult: Boolean = false): Connectable
data class Wall(var from: Connectable, var to: Connectable, val drawBridge: DrawBridge?)

class CastleWallNotConnectedException(message: String) : Exception(message)
