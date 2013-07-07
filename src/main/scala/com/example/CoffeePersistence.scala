package com.example

import com.example.domain.{Next, Order}

trait CoffeePersistence {
  def persist(order: Order, paymentLinkFn: Int => String): (Int, Order)

  def update(counter: Int, porder: Order): Unit

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

  def update(counter: Int, porder: Order) {
    map -= counter
    map += (counter -> porder)
  }

  def retrieve(id: Int) = map.get(id)
}
