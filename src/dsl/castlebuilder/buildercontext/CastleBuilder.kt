package dsl.castlebuilder.buildercontext

import dsl.castlebuilder.model.Connectable
import dsl.castlebuilder.model.StringSymbolTable

fun main(args : Array<String>) {
    CastleDsl().build()
}

class CastleDsl {
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

}

class CastleBuilder {
    private var keep: KeepBuilder? = null
    private var towers = mutableListOf<TowerBuilder>()
    private var walls = mutableListOf<WallBuilder>()

    fun keep() : KeepBuilder {
        val keepBuilder = KeepBuilder(this)
        keep = keepBuilder
        return keepBuilder
    }

    fun tower(name: String) : TowerBuilder {
        val towerBuilder = TowerBuilder(this, name)
        towers.add(towerBuilder)
        return towerBuilder
    }

    fun connect(from: String, to: String, drawBridge: DrawBridge? = null)
            : CastleBuilder {
        walls.add(WallBuilder(this, from, to, drawBridge))
        return this
    }

    fun build() : Castle {
        val allTowers =  towers.map { it.build() }

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

open class CastleBuilderHolder(val castleBuilder: CastleBuilder)

class TowerBuilder(
        castleBuilder: CastleBuilder,
        override val name: String,
        private var hasCatapult: Boolean = false
): CastleBuilderHolder(castleBuilder), Connectable {
    fun catapult(): TowerBuilder {
        hasCatapult = true
        return this
    }

    fun wall() : WallBuilder {
        return WallBuilder(castleBuilder, name)
    }

    fun build() : Tower {
        return Tower(name, hasCatapult)
    }
}

class WallBuilder(private val castleBuilder: CastleBuilder,
                  private val from: String,
                  var to: String? = null,
                  private var drawBridge: DrawBridge? = null
) {
    fun drawbridge(): WallBuilder {
        drawBridge = DrawBridge()
        return this
    }

    fun tower(name: String) : TowerBuilder {
        to = name
        return castleBuilder.tower(name)
    }

    fun build(symbols: StringSymbolTable<Connectable>) : Wall {
        to?.let {
            return Wall(symbols.lookup(from), symbols.lookup(it), drawBridge)
        }

        throw CastleWallNotConnectedException("wall ${from} needs and end")
    }
}

class KeepBuilder(private val castleBuilder: CastleBuilder,
                  val name: String = "keep") {
    fun tower(to: String): KeepBuilder {
        castleBuilder.connect(name, to)
        return this
    }

    fun build() : Keep {
        return Keep(name)
    }
}

data class Castle(var keep: Keep?, var towers: List<Tower>, var walls: List<Wall>)

data class Keep(override var name: String = "keep"): Connectable
data class Tower(override var name:String, var hasCatapult: Boolean): Connectable
data class Wall(var from: Connectable, var to: Connectable, var drawBridge: DrawBridge?)

class DrawBridge

class CastleWallNotConnectedException(message: String) : Exception(message)
