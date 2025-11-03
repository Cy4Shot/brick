package brick.tmpl

import java.lang.annotation._
import scala.quoted.Quotes
import brick.parse.tmpl.Type._
import brick.conc.BricksCtx

type TypedVal = (BricksCtx => Any, Type)
type SymbolTable = Map[String, TypedVal]

sealed trait Node:
  def flatten(prefix: List[String] = Nil): SymbolTable

case class Leaf(value: () => TypedVal) extends Node:
  def flatten(prefix: List[String]): SymbolTable =
    Map(prefix.filter(_.nonEmpty).mkString(".") -> value())

case class Branch(children: Map[String, Node]) extends Node:
  def flatten(prefix: List[String]): SymbolTable =
    children.flatMap { case (name, node) =>
      node.flatten(prefix :+ name)
    }

class Builder:
  private var children = Map.empty[String, Node]

  def apply(name: String)(body: Builder ?=> Unit): Unit =
    val b = new Builder
    body(using b)
    children += name -> Branch(b.children)

  def value(body: => Any, ty: Type): Unit =
    children += "" -> Leaf(() => ((_) => body, ty))

  def dynamic(f: BricksCtx => Any, ty: Type)(using b: Builder): Unit =
    b.children += "" -> Leaf(() => (f, ty))

  def build(): Branch = Branch(children.filterNot(_._1 == ""))

object BrickTemplate:
  def apply(body: Builder ?=> Unit): Node =
    given b: Builder = new Builder
    body
    b.build()

def value(body: => Any, ty: Type)(using b: Builder): Unit =
  b.value(body, ty)

def dynamic(f: BricksCtx => Any, ty: Type)(using b: Builder): Unit =
  b.dynamic(f, ty)

def name(name: String)(body: Builder ?=> Unit)(using b: Builder): Unit =
  b.apply(name)(body)

extension (sc: String)
  def ~(body: Builder ?=> Unit)(using b: Builder): Unit =
    b.apply(sc)(body)

extension (scs: Seq[String])
  def ~(body: Builder ?=> Unit)(using b: Builder): Unit =
    scs.foreach(s => b.apply(s)(body))
