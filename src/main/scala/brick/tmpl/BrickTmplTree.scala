package brick.tmpl

import java.lang.annotation._
import scala.quoted.Quotes

sealed trait Node:
  def flatten(prefix: List[String] = Nil): Map[String, Any]

case class Leaf(value: () => Any) extends Node:
  def flatten(prefix: List[String]): Map[String, Any] =
    Map(prefix.filter(_.nonEmpty).mkString(".") -> value())

case class Branch(children: Map[String, Node]) extends Node:
  def flatten(prefix: List[String]): Map[String, Any] =
    children.flatMap { case (name, node) =>
      node.flatten(prefix :+ name)
    }

class Builder:
  private var children = Map.empty[String, Node]

  def apply(name: String)(body: Builder ?=> Unit): Unit =
    val b = new Builder
    body(using b)
    children += name -> Branch(b.children)

  def value(body: => Any): Unit =
    children += "" -> Leaf(() => body)

  def build(): Branch = Branch(children.filterNot(_._1 == ""))

object BrickTemplate:
  def apply(body: Builder ?=> Unit): Node =
    given b: Builder = new Builder
    body
    b.build()

def value(body: => Any)(using b: Builder): Unit =
  b.value(body)

def name(name: String)(body: Builder ?=> Unit)(using b: Builder): Unit =
  b.apply(name)(body)

extension (sc: String)
  def ~(body: Builder ?=> Unit)(using b: Builder): Unit =
    b.apply(sc)(body)
