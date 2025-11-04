package brick.tmpl

import java.lang.annotation._
import scala.quoted.Quotes
import brick.parse.tmpl.Type._
import brick.conc.BricksCtx

type TypedVal = (BricksCtx => Any, Type)
type WildcardHandler = String => BricksCtx => Any
type SymbolTable = Map[String, Either[TypedVal, (WildcardHandler, Type)]]

sealed trait Node:
  def flatten(prefix: List[String] = Nil): SymbolTable

case class Leaf(value: () => TypedVal) extends Node:
  def flatten(prefix: List[String]): SymbolTable =
    Map(prefix.filter(_.nonEmpty).mkString(".") -> Left(value()))

case class Branch(children: Map[String, Node]) extends Node:
  def flatten(prefix: List[String]): SymbolTable =
    children.flatMap { case (name, node) =>
      node.flatten(prefix :+ name)
    }

case class BranchWithValue(value: () => TypedVal, children: Map[String, Node])
    extends Node:
  def flatten(prefix: List[String]): SymbolTable =
    val selfEntry = Map(
      prefix.filter(_.nonEmpty).mkString(".") -> Left(value())
    )
    val childEntries = children.flatMap { case (name, node) =>
      node.flatten(prefix :+ name)
    }
    selfEntry ++ childEntries

case class Wildcard(ty: Type, handler: String => BricksCtx => Any) extends Node:
  def flatten(prefix: List[String]): SymbolTable =
    Map(prefix.filter(_.nonEmpty).mkString(".") + ".*" -> Right((handler, ty)))

class Builder:
  private var children = Map.empty[String, Node]
  private var selfValue: Option[() => TypedVal] = None

  def apply(name: String)(body: Builder ?=> Unit): Unit =
    val b = new Builder
    body(using b)
    val node = b.selfValue match {
      case Some(value) => BranchWithValue(value, b.children)
      case None        => Branch(b.children)
    }
    children += name -> node

  def value(body: => Any, ty: Type): Unit =
    selfValue = Some(() => ((_) => body, ty))

  def dynamic(f: BricksCtx => Any, ty: Type): Unit =
    selfValue = Some(() => (f, ty))

  def wildcard(ty: Type)(handler: String => BricksCtx => Any): Unit =
    children += "" -> Wildcard(ty, handler)

  def build(): Node =
    selfValue match {
      case Some(value) => BranchWithValue(value, children.filterNot(_._1 == ""))
      case None        => Branch(children.filterNot(_._1 == ""))
    }

object BrickTemplate:
  def apply(body: Builder ?=> Unit): Node =
    given b: Builder = new Builder
    body
    b.build()

def value(body: => Any, ty: Type)(using b: Builder): Unit =
  b.value(body, ty)

def dynamic(f: BricksCtx => Any, ty: Type)(using b: Builder): Unit =
  b.dynamic(f, ty)

def wildcard(ty: Type)(handler: String => BricksCtx => Any)(using
    b: Builder
): Unit =
  b.wildcard(ty)(handler)

def name(name: String)(body: Builder ?=> Unit)(using b: Builder): Unit =
  b.apply(name)(body)

extension (sc: String)
  def ~(body: Builder ?=> Unit)(using b: Builder): Unit =
    b.apply(sc)(body)

extension (scs: Seq[String])
  def ~(body: Builder ?=> Unit)(using b: Builder): Unit =
    scs.foreach(s => b.apply(s)(body))
