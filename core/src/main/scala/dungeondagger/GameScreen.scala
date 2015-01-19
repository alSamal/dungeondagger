package dungeondagger

import com.badlogic.gdx.graphics.g2d.{Sprite, Batch}
import com.badlogic.gdx._
import com.badlogic.gdx.graphics.{Texture, GL20}
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.scenes.scene2d.actions.{DelayAction, SequenceAction, MoveByAction}
import com.badlogic.gdx.scenes.scene2d.{Actor, Stage}

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
  var gen = Generator.newGen()

  val Height = 150
  val Width = 180

  def generateMap = Generator.terrain(Width, Height, textures.size - 1, gen).map{
    Terrains.All
  }

  var map: Array[Terrain] = generateMap

  var person = 0
  val personTexture = new Texture(Gdx.files.internal("data/hexagonTiles/Tiles/alienPink.png"))
//  val personTexture = new Texture(Gdx.files.internal("data/hexagonTiles/village.gif"))
  val castleTexture = new Texture(Gdx.files.internal("data/hexagonTiles/village.gif"))
  val fishTexture = new Texture(Gdx.files.internal("data/hexagonTiles/fish.png"))
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

    override def draw(batch:Batch, alpha:Float){
      batch.draw(texture,getX,getY)
      if (flower)
        batch.draw(flowerTexture, getX + 35, getY + 35)
      if (tree)
        batch.draw(treeTexture, getX + 35, getY + 35)
      if (fish)
        batch.draw(fishTexture, getX + 10, getY + 35, 40, 40)
      if (hasPerson)
        batch.draw(personSprite, getX, getY + 35)

    }
  }

  def tActors = Range(0, Height).map{ i =>
    Range(0, Width).map { j =>
      val tileId = i * Width + j
      val terrain = map(tileId)
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
    tileActors(person).hasPerson = false
    person = toTileId
    val newTile = tileActors(person)
    newTile.hasPerson = true
    stage.getViewport.apply(false)
    stage.getViewport.getCamera.position.set(newTile.getX, newTile.getY + 35, 0)
  }

  movePerson((tileActors.size + Width ) / 2)

  def wobble(): Unit ={
    val center = tileActors(person)
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

  override def render(delta: Float) {
    Gdx.gl.glClearColor(1, 1, 1, 1)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

    stage.act(Gdx.graphics.getDeltaTime)
    stage.draw()
  }

  private def canPass(newPosition: Int) = map(newPosition).passThrough

  override def keyDown(keycode: Int): Boolean = {
    keycode match {
      case(Input.Keys.UP) if person + Width < Height * Width && (canPass(person + Width) || !canPass(person)) => movePerson(person + Width)
      case(Input.Keys.DOWN) if person - Width >= 0 && (canPass(person - Width)|| !canPass(person)) => movePerson(person - Width)
      case(Input.Keys.RIGHT) if person % Width != Width - 1 && (canPass(person + 1)|| !canPass(person)) => movePerson(person + 1)
      case(Input.Keys.LEFT) if person % Width != 0  && (canPass(person - 1)|| !canPass(person)) => movePerson(person - 1)
      case(Input.Keys.R) =>
        gen = Generator.newGen()
        map = generateMap
        tileActors = tActors
        stage.clear()
        tileActors.reverse foreach stage.addActor
      case _ => wobble()
    }
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
