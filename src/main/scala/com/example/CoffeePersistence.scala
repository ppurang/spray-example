package com.example

import com.example.domain.{Next, Order}
import scala.slick.jdbc.meta.MTable

trait CoffeePersistence {
  def persist(order: Order, paymentLinkFn: Int => String): (Int, Order)

  def update(id: Int, order: Order): Unit

  def retrieve(id: Int): Option[Order]
}

trait HashMapCoffeePersistence extends CoffeePersistence {
  private var map = collection.mutable.Map[Int, Order]()
  private var counter = 0

  def persist(order: Order, paymentLinkFn: Int => String) = {
    counter = counter + 1
    val porder = order.copy(next = Option(Next("payment", paymentLinkFn(counter), "application/vnd.coffee+json")))
    map += (counter -> porder)
    (counter, porder)
  }

  def update(id: Int, order: Order) {
    map -= id
    map += (id -> order)
  }

  def retrieve(id: Int) = map.get(id)
}

trait SlickH2CoffeePersistence extends CoffeePersistence {
  self: ConfigProvider =>

  import scala.slick.driver.H2Driver.simple._

  private val db = Database.forURL(
    url = config getString "persistence.db.url",
    driver = config getString "persistence.db.driver")

  def persist(order: Order, paymentLinkFn: (Int) => String) = db.withSession { implicit s =>
    val id = Orders returning Orders.id insert (None -> order)
    val orderToUpdate = order.copy(next = Option(Next("payment", paymentLinkFn(id), "application/vnd.coffee+json")))

    update(id, orderToUpdate)

    id -> orderToUpdate
  }

  def update(id: Int, order: Order) = db withSession { implicit s: Session =>
    ordersByIdQuery(id).update(Some(id) -> order)
  }

  def retrieve(id: Int) = db withSession { implicit s =>
    ordersByIdQuery(id).firstOption().map(_._2)
  }

  private def ordersByIdQuery(id: Int) = for {
    order <- Orders if order.id is id
  } yield order

  private def initDb() {
    def databaseAlreadyCreated(implicit session: Session) =
      MTable.getTables(Orders.tableName).list().nonEmpty

    def createTables(implicit s: Session) {
      Orders.ddl.create
    }

    def recreateTables(implicit s: Session) {
      Orders.ddl.drop
      createTables
    }

    db.withSession{ implicit s: Session =>
      if (!databaseAlreadyCreated) createTables
      else if (config getBoolean "persistence.db.recreateOnStartup") recreateTables
    }
  }

  private object Orders extends Table[(Option[Int], Order)]("order") {
    def id = column[Int]("order_id", O.PrimaryKey, O.AutoInc)
    def drink = column[String]("drink")
    def cost = column[Option[Double]]("cost")
    def status = column[Option[String]]("status")

    def nextRel = column[Option[String]]("next_rel")
    def nextUri = column[Option[String]]("next_uri")
    def nextType = column[Option[String]]("next_type")

    def * = id.? ~ drink ~ cost ~ status ~ nextRel ~ nextUri ~ nextType <> (
      _ match {case (id, drink, cost, status, nextRel, nextUri, nextType) =>
        (id, Order(drink, cost, nextRel.map(x => Next(nextRel.get, nextUri.get, nextType.get)), status))},
      {case (id, o) =>
        Some((id, o.drink, o.cost, o.status, o.next.map(_.rel), o.next.map(_.uri), o.next.map(_.`type`)))}
    )
  }

  initDb()
}
