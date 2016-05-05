package com.github.scalalab3.logs.storage

import java.util.HashMap
import shapeless._, record._


object ToMap {
  case class Empty()

  implicit def mapToHashMap(m: Map[String, Any]): HashMap[String, Any] = {
    val out:HashMap[String, Any] = new HashMap()
    m.foreach(kv => out.put(kv._1, kv._2))
    out
  }

  implicit def toHashMap[T, R <: HList, K <: Symbol, V](a: T)
    (implicit
      lgen: LabelledGeneric.Aux[T, R],
      toMap: ops.record.ToMap.Aux[R, K, V]
    ): HashMap[String, Any] = {

    val gen = LabelledGeneric[T]
    gen.to(a).toMap.filter { case (k, None) => false; case _ => true }
    .map {
      case (k, Some(v)) => k.name -> v
      case (k, v) => k.name -> v
    }
  }
}