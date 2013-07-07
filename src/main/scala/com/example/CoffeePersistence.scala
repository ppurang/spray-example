package com.example

import com.example.domain.{Next, Order}
import scala.slick.jdbc.meta.MTable

trait CoffeePersistence {
  def persist(order: Order, paymentLinkFn: Int => String): (Int, Order)

  def update(order: Order): Unit

  def retrieve(id: Int): Option[Order]
}

trait HashMapCoffeePersistence extends CoffeePersistence {
  private var map = collection.mutable.Map[Int, Order]()
  private var counter = 0

  def persist(order: Order, paymentLinkFn: Int => String) = {
    counter = counter + 1
    val porder = order.copy(id = Some(counter), next = Option(Next("payment", paymentLinkFn(counter), "application/vnd.coffee+json")))
    map += (counter -> porder)
    (counter, porder)
  }

  def update(order: Order) {
    map -= order.id getOrElse (throw new IllegalArgumentException("You can't update order without ID"))
    map += (order.id.get -> order)
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
    val id = Orders returning Orders.id insert order
    val orderToUpdate = order.copy(id = Some(id), next = Option(Next("payment", paymentLinkFn(id), "application/vnd.coffee+json")))

    update(orderToUpdate)

    (id, orderToUpdate)
  }

  def update(order: Order) = db withSession { implicit s: Session =>
    ordersByIdQuery(order.id getOrElse (throw new IllegalArgumentException("You can't update order without ID")))
      .update(order)
  }

  def retrieve(id: Int) = db withSession { implicit s =>
    ordersByIdQuery(id).firstOption()
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

  private object Orders extends Table[Order]("order") {
    def id = column[Int]("order_id", O.PrimaryKey, O.AutoInc)
    def drink = column[String]("drink")
    def cost = column[Option[Double]]("cost")
    def status = column[Option[String]]("status")

    def nextRel = column[Option[String]]("next_rel")
    def nextUri = column[Option[String]]("next_uri")
    def nextType = column[Option[String]]("next_type")

    // OH, I like tuples, but not to this extent :)
    // TODO: find better way to map (maybe extract `next` to it's own table)
    def * = id.? ~ drink ~ cost ~ status ~ nextRel ~ nextUri ~ nextType <>
      (r => Order(r._1, r._2, r._3, r._5.map(x => Next(r._5.get, r._6.get, r._7.get)), r._4),
        o => Some((o.id, o.drink, o.cost, o.status, o.next.map(_.rel), o.next.map(_.uri), o.next.map(_.`type`))))
  }

  initDb()
}
