package dungeondagger

import com.badlogic.gdx.graphics.g2d.{Sprite, Batch}
import com.badlogic.gdx._
import com.badlogic.gdx.graphics.{Texture, GL20}
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.scenes.scene2d.actions.{DelayAction, SequenceAction, MoveByAction}
import com.badlogic.gdx.scenes.scene2d.{Actor, Stage}

import scala.collection.mutable
import scala.util.Random

class GameScreen(game: Game) extends DefaultScreen(game) with InputProcessor {
  def path(hexName: String) =
    s"data/hexagonTiles/Tiles/tile$hexName.png"

  def flowersPath(color: String) =
    s"data/hexagonTiles/Tiles/flower$color.png"

  def treePath(treeType: String) =
    s"data/hexagonTiles/Tiles/tree$treeType.png"

  println(Gdx.files.getExternalStoragePath)
  val textures = Array("Water_full", "Sand", "Dirt", "Grass", "Autumn", "Lava", "Magic", "Rock", "Stone")
    .map(path)
    .map(Gdx.files.local)
    .map(new Texture(_))

  val flowers = Array("Blue", "Red", "Green", "Red", "White", "Yellow")
    .map(flowersPath)
    .map(Gdx.files.local)
    .map(new Texture(_))

  val trees = Array("Autumn_high", "Autumn_low", "Autumn_mid",
    "Blue_high", "Blue_low", "Blue_mid",
    "Cactus_1", "Cactus_2", "Cactus_3",
    "Green_high", "Green_low", "Green_mid")
    .map(treePath)
    .map(Gdx.files.local)
    .map(new Texture(_))


  Gdx.input.setInputProcessor(this)

  val rand = new Random()

  val person = new PlayerAgent()
  var personPos = (151 * 150) / 2
  val world = new World(width = 150, height = 150, agents = mutable.Map(person -> personPos))
  def w = world.width
  def h = world.height

  val personTexture = new Texture(Gdx.files.internal("data/hexagonTiles/Tiles/alienPink.png"))
//  val personTexture = new Texture(Gdx.files.internal("data/hexagonTiles/village.gif"))
  val castleTexture = new Texture(Gdx.files.internal("data/hexagonTiles/village.gif"))
  val fishTexture = new Texture(Gdx.files.internal("data/hexagonTiles/fish.png"))
  val campfireTexture = new Texture(Gdx.files.internal("data/hexagonTiles/campfire.png"))
  val personSprite = new Sprite(personTexture)
  personSprite.setAlpha(1)

  val stage:Stage = new Stage()

  override def dispose() {
    stage.dispose()
    personTexture.dispose()
    textures.map(_.dispose())
  }

  class HexTile(texture: Texture, val terrain: Terrain) extends Actor {
    var started = false
    var hasPerson = false
    val flower = (terrain != Terrains.Water) && rand.nextInt(10) == 0
    val flowerTexture = flowers(rand.nextInt(flowers.size))
    val tree = (terrain != Terrains.Water) && !flower && rand.nextInt(15) == 0
    val treeTexture = trees(rand.nextInt(trees.size))
    val fish = terrain == Terrains.Water && rand.nextInt(10) == 0
    val campfire = terrain == Terrains.Grass && !flower && !tree && rand.nextInt(300) == 0

    override def draw(batch:Batch, alpha:Float){
      Range(0, terrain.height + 1).foreach { i =>
        batch.draw(texture, getX, getY + i * 24 )
      }
      val attrY = getY + terrain.height * 24 + 35
      if (flower)
        batch.draw(flowerTexture, getX + 35, attrY)
      if (tree)
        batch.draw(treeTexture, getX + 35, attrY)
      if (fish)
        batch.draw(fishTexture, getX + 10, attrY, 40, 40)
      if (campfire)
        batch.draw(campfireTexture, getX + 10, attrY, 50, 50)
      if (hasPerson)
        batch.draw(personSprite, getX, attrY)

    }
  }

  def tActors = Range(0, w).map{ i =>
    Range(0, h).map { j =>
      val tileId = i * w + j
      val terrain = world.map(tileId)
      val t = textures(terrain.id)
      val x = j * 65 + (i % 2) * 32
      val tile = new HexTile(t, terrain)
      tile.setPosition(x,i * 49)
      tile
    }
  }.flatten


  var tileActors = tActors

  tileActors.reverse foreach stage.addActor

  tileActors(0).hasPerson = true

  def movePerson(toTileId: Int): Unit ={
    tileActors(personPos).hasPerson = false
    personPos = toTileId
    val newTile = tileActors(personPos)
    newTile.hasPerson = true
    stage.getViewport.apply(false)
    stage.getViewport.getCamera.position.set(newTile.getX, newTile.getY + 35, 0)
  }

  movePerson(personPos)

  def wobble(): Unit ={
    val center = tileActors(personPos)
    tileActors foreach { a =>
      val dx = center.getX - a.getX
      val dy = center.getY - a.getY
      val r2 = dx * dx + dy * dy
//      val r = Math.sqrt(r2).toFloat
      val there = new MoveByAction()
      val back = new MoveByAction()
      there.setDuration(1.7f)
      back.setDuration(1.7f)
      val delay = new DelayAction()
      delay.setDuration(r2 / 100000)
      there.setInterpolation(Interpolation.circleOut)
      back.setInterpolation(Interpolation.circleIn)
      val h = 10000000 / (50000 + r2)
      there.setAmountY(h)
      back.setAmountY(-h)
      val action = new SequenceAction(delay,there,back)
      a.addAction(action)
    }
  }

  def processWorldEvents():Unit = {
    world.step() foreach {
      case AgentMoved(a, to) => movePerson(to)
      case _ =>
    }
  }

  override def render(delta: Float) {
    Gdx.gl.glClearColor(1, 1, 1, 1)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

    stage.act(Gdx.graphics.getDeltaTime)
    stage.draw()
  }


  override def keyDown(keycode: Int): Boolean = {
    keycode match {
      case(Input.Keys.ESCAPE) => System.exit(0)
      case(Input.Keys.UP) => person.go(0)
      case(Input.Keys.DOWN) => person.go(2)
      case(Input.Keys.RIGHT) => person.go(1)
      case(Input.Keys.LEFT) => person.go(3)
      case(Input.Keys.R) =>
        world.regenerateMap()
        tileActors = tActors
        stage.clear()
        tileActors.reverse foreach stage.addActor
      case(Input.Keys.W) => wobble()
      case _ =>
    }
    processWorldEvents()
    true
  }

  override def mouseMoved(screenX: Int, screenY: Int): Boolean = false

  override def keyTyped(character: Char): Boolean = false

  override def touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = false

  override def keyUp(keycode: Int): Boolean = false

  override def scrolled(amount: Int): Boolean = false

  override def touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = false

  override def touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean = false
}
